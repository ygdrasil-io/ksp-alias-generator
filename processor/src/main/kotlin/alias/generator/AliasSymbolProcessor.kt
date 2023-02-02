@file:OptIn(KspExperimental::class)

package alias.generator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.OutputStream

internal class AliasSymbolProcessor(
    options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val sourcePackage = options["sourcePackage"]
        ?: "sourcePackage must be provided to alias generator KSP plugin"
    private val targetPackage = options["targetPackage"]
        ?: "targetPackage must be provided to alias generator KSP plugin"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start to process symbols on $sourcePackage")

        resolver.getAllFiles()
            .filterFromSourcePackage()
            .createTargetFile(resolver)
            .filterNotNull()
            .injectDeclarations()
            .forEach(OutputStream::close)

        return listOf()
    }

    private fun Sequence<Pair<Sequence<KSDeclaration>, OutputStream>>.injectDeclarations()
            = map { (declarations, outputStream) ->

        declarations.forEach { declaration ->
            logger.info("visit symbol ${declaration.qualifiedName?.getQualifier()} of type ${declaration::class.simpleName}")

            outputStream += when (declaration) {
                is KSPropertyDeclaration -> "val ${declaration.name} = ${declaration.fullName}"
                else -> "typealias ${declaration.name} = ${declaration.fullName}"
            }

            outputStream += "\n"
        }

        outputStream

    }

    private fun Sequence<KSFile>.filterFromSourcePackage() =
        filter { it.packageName.asString().startsWith(sourcePackage) }

    private fun Sequence<KSFile>.createTargetFile(resolver: Resolver): Sequence<Pair<Sequence<KSDeclaration>, OutputStream>?> = map {
        try {
            val newPackage = targetPackage + it.packageName.asString().replace(sourcePackage, "")
            it.declarations to codeGenerator.createNewFile(
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = newPackage,
                fileName = it.fileName
            ).also { stream ->
                stream += "package $newPackage\n"
            }
        } catch (error: FileAlreadyExistsException) {
            logger.warn("process multiple time file ${it.fileName}")
            null
        }
    }

}

private operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
}

private val KSDeclaration.name: String
    get() = simpleName.asString()
private val KSDeclaration.fullName: String
    get() = "${packageName.asString()}.$name"

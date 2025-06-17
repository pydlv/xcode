package org.giraffemail.xcode

import org.giraffemail.xcode.cli.TranspilerCli

fun main(args: Array<String>) {
    val cli = TranspilerCli()
    cli.run(args)
}

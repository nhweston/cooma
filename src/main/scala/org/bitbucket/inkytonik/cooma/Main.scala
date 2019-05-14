/*
 * This file is part of Cooma.
 *
 * Copyright (C) 2019 Anthony M Sloane, Macquarie University.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.bitbucket.inkytonik.cooma

import org.bitbucket.inkytonik.kiama.util.CompilerBase
import syntax.CoomaParserSyntax.{ASTNode, Program}

object Main extends CompilerBase[ASTNode, Program, Config] {

    import org.bitbucket.inkytonik.kiama.output.PrettyPrinterTypes.Document
    import org.bitbucket.inkytonik.kiama.util.Source
    import org.bitbucket.inkytonik.kiama.util.Messaging.Messages
    import syntax.CoomaParser
    import syntax.CoomaParserPrettyPrinter
    import syntax.CoomaParserPrettyPrinter.{any, layout, show}
    import syntax.CoomaParserSyntax.ErrR

    val name = "cooma"

    def createConfig(args : Seq[String]) : Config =
        new Config(args)

    override def compileFiles(config : Config) {
        val args = config.filenames()
        if (args.length >= 1)
            compileFile(args(0), config)
    }

    override def makeast(source : Source, config : Config) : Either[Program, Messages] = {
        val p = new CoomaParser(source, positions)
        val pr = p.pProgram(0)
        if (pr.hasValue)
            Left(p.value(pr).asInstanceOf[Program])
        else
            Right(Vector(p.errorToMessage(pr.parseError)))
    }

    def process(source : Source, prog : Program, config : Config) {
        if (config.coomaASTPrint())
            config.output().emitln(layout(any(prog)))

        val ir = Compiler.compile(prog)
        if (config.irPrint())
            config.output().emitln(show(ir, 5))
        if (config.irASTPrint())
            config.output().emitln(layout(any(ir)))

        val args = config.filenames().tail
        Interpreter.interpret(ir, args) match {
            case ErrR(msg) =>
                config.output().emitln(s"cooma: $msg")
            case v =>
                if (config.resultPrint()) {
                    val resultStr = PrettyPrinter.showValue(v)
                    config.output().emitln(resultStr)
                }
        }
    }

    override def format(prog : Program) : Document =
        CoomaParserPrettyPrinter.format(prog, 5)

}

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

import org.bitbucket.inkytonik.kiama.util.REPLBase

class REPLDriver extends REPLBase[Config] {

    import org.bitbucket.inkytonik.cooma.BuildInfo
    import org.bitbucket.inkytonik.cooma.CoomaParserPrettyPrinter.{any, layout}
    import org.bitbucket.inkytonik.cooma.CoomaParserSyntax._
    import org.bitbucket.inkytonik.cooma.IR.showTerm
    import org.bitbucket.inkytonik.cooma.Runtime.showRuntimeValue
    import org.bitbucket.inkytonik.kiama.util.{Console, StringConsole, Source}

    def createConfig(args : Seq[String]) : Config =
        new Config(args)

    val banner = s"${BuildInfo.name} ${BuildInfo.version} REPL \n\nEnter definitions or expressions (:help for commands)"

    override val prompt = "\ncooma> "

    /**
     * Runtime environment that keeps track of previously bound values.
     */
    var currentDynamicEnv : Env = NilE()

    /**
     * Counter of expression results.
     */
    var nResults = 0

    /**
     * Initialise REPL state.
     */
    def initialise() {
        currentDynamicEnv = NilE()
        nResults = 0
    }

    /**
     * Start the REPL
     */
    override def driver(args : Seq[String]) {
        initialise()
        super.driver(args)
    }

    /**
     * Extractor for commands, splits the line into separate words.
     */
    object Command {
        def unapply(line : String) : Option[Seq[String]] = {
            Some((line.trim split ' ').toIndexedSeq)
        }
    }

    override def processline(source : Source, console : Console, config : Config) : Option[Config] = {

        import org.bitbucket.inkytonik.kiama.util.StringSource
        import scala.collection.mutable.ListBuffer

        def help() {
            config.output().emit("""
                |exp                    evaluate exp, print value
                |val x = exp            add new value definition
                |def f(x : Int) = exp   add new function definition
                |:help                  print this message
                |:lines                 enter multiple separate input lines until :end
                |:paste                 enter single multi-line input until :end
                |:quit                  quit the REPL (also Control-D)
                |""".stripMargin)
        }

        def getLines() : String = {
            val buf = ListBuffer[String]()
            var line = console.readLine("")
            while (line.trim != ":end") {
                buf.append(line + "\n")
                line = console.readLine("")
            }
            buf.mkString
        }

        /*
         * Embed an entry in a program and process it.
         */
        def processEntry(input : REPLInput) =
            input match {

                case REPLExpression(e) =>
                    val i = s"res$nResults"
                    nResults = nResults + 1
                    process(Program(e), i, true, config)

                case REPLDef(fd @ Def(i, _, _)) =>
                    process(
                        Program(Block(LetFun(Vector(fd), Return(IdnUse(i))))),
                        i, false, config
                    )

                case REPLVal(Val(i, e)) =>
                    process(Program(e), i, true, config)

            }

        /*
         * Enter a REPL input line by processing it, ignoring whitespace lines.
         */
        def enterline(line : String) {
            val source = new StringSource(line)
            val p = new CoomaParser(source, positions)
            val pr = p.pWhitespace(0)
            if (!pr.hasValue) {
                val pr = p.pREPLInput(0)
                if (pr.hasValue)
                    processEntry(p.value(pr).asInstanceOf[REPLInput])
                else
                    config.output().emitln(p.formatParseError(pr.parseError, false))
            }
        }

        source.content match {
            case Command(Seq(":help")) =>
                help()
            case Command(Seq(":lines")) =>
                processconsole(new StringConsole(getLines()), "", config)
            case Command(Seq(":paste")) =>
                enterline(getLines())
            case Command(Seq(":quit")) =>
                return None
            case line =>
                enterline(line)
        }

        Some(config)

    }

    /**
     * Process the AST from the user's entered text.
     */
    def process(program : Program, i : String, printValue : Boolean, config : Config) {

        // Pretty print the abstract syntax tree
        if (config.coomaASTPrint())
            config.output().emitln(layout(any(program), 5))

        // Translate the source tree to IR
        val ir = Compiler.compileStandalone(program)

        // Pretty print the program's IR
        if (config.irPrint())
            config.output().emitln(showTerm(ir, 5))
        if (config.irASTPrint())
            config.output().emitln(layout(any(ir), 5))

        // Run the IR and collect result
        val args = config.filenames()
        val interpreter = new Interpreter(config)
        val result = interpreter.interpret(ir, currentDynamicEnv, args)

        // Update the environments so the new result can be used later
        currentDynamicEnv = ConsVE(currentDynamicEnv, i, result)

        // Process the result value
        if (printValue)
            config.output().emitln(s"$i = ${showRuntimeValue(result)}")
        else
            config.output().emitln(i)

    }

}
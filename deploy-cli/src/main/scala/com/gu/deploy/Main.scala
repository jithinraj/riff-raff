package com.gu.deploy

import java.io.File
import scopt.OptionParser
import json.JsonReader

object Main extends App {

  object Config {
    var project: File = _
    var recipe: String = "default"
    var verbose = false

    def project_= (s: String) {
      val f = new File(s)
      if (!f.exists() || !f.isFile) sys.error("File not found.")
      project = f
    }
  }

  object CommandLineOutput extends Output {
    def verbose(s: => String) { if (Config.verbose) Console.out.println(s) }
    def info(s: => String) { Console.out.println(s) }
    def warn(s: => String) { Console.out.println("WARN: " + s) }
    def error(s: => String) { Console.err.println(s) }
  }


  val programName = "deploy"
  val programVersion = "*PRE-ALPHA*"

  val parser = new OptionParser(programName, programVersion) {
    arg("<project>", "json deploy project file", { p => Config.project = p })
    opt("r", "recipe", "recipe to execute (default: 'default')", { r => Config.recipe = r })
    opt("v", "verbose", "verbose logging", { Config.verbose = true } )
  }

  Log.current.withValue(CommandLineOutput) {
    if (parser.parse(args)) {
      try {
        Log.info("%s %s" format (programName, programVersion))

        Log.info("Loading project file...")
        val project = JsonReader.parse(Config.project)

        Log.verbose("Loaded: " + project)

        Log.info("Loading deployinfo... (CURRENTLY STUBBED)")
        val dummyDeployInfo = List(Host("localhost").role("mac"), Host("localhost").role("mac"))

        Log.info("Resolving...")
        val tasks = Resolver.resolve(project, Config.recipe, dummyDeployInfo)

        Log.info("Tasks to execute: ")
        tasks.zipWithIndex.foreach { case (task, idx) => Log.info(" %d. %s" format (idx + 1, task)) }

        Log.info("Executing...")
        tasks.foreach { task =>
          Log.info(" Executing %s..." format task)
          task.execute()
        }

        Log.info("Done")

      } catch {
        case e: Exception =>
          Log.error("FATAL: " + e)
          if (Config.verbose) {
            e.printStackTrace()
          }
      }
    }
  }
}
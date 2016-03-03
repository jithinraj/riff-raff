package magenta.deployment_type

import java.io.File
import magenta.MessageBroker
import magenta.tasks.UpdateLambda

object Lambda extends DeploymentType  {
  val name = "aws-lambda"
  val documentation =
    """
      |Provides one deploy action, `updateLambda`, that runs Lambda Update Function Code using the package files which can be specified per stage.
    """.stripMargin
  

  //required configuration, you cannot upload without setting these
  val functions = Param[Map[String, Map[String, String]]]("functions",
    documentation =
      """
        |In order for this to work, magenta must have credentials that are able to perform `lambda:UpdateFunctionCode` on the specified resources.
        |
        |Map of Stage to Lambda functions. `name` is the Lambda `FunctionName`. The `filename` field is optional and if not specified defaults to `lambda.zip`
        |e.g.
        |
        |        "functions": {
        |          "CODE": {
        |           "name": "myLambda-CODE",
        |           "filename": "myLambda-CODE.zip",
        |          },
        |          "PROD": {
        |           "name": "myLambda-PROD",
        |           "filename": "myLambda-PROD.zip",
        |          }
        |        }
      """.stripMargin
  ).default(Map.empty)

  def perAppActions = {
    case "updateLambda" => (pkg) => (resourceLookup, parameters, stack) => {
      implicit val keyRing = resourceLookup.keyRing(parameters.stage, pkg.apps.toSet, stack)
      val stage = parameters.stage.name

      val functionDefinition = functions(pkg).getOrElse(stage, MessageBroker.fail(s"Function not defined for stage $stage"))
      val functionName = functionDefinition.getOrElse("name", MessageBroker.fail(s"Function name not defined for stage $stage"))
      val fileName = functionDefinition.getOrElse("filename", "lambda.zip")
      val maybeListOfTasks = for {
        definition <- functionDefinition
      } yield {
        UpdateLambda(new File(s"${pkg.srcDir.getPath}/$fileName"), functionName)
      }

      maybeListOfTasks.toList

    }
  }
}
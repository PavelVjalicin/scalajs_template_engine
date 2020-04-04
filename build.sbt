import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.scalaJSUseMainModuleInitializer
import sbtcrossproject.CrossType
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbt.Keys.{ scalaVersion, version}



lazy val server = (project in file("server"))
    .settings(
        name := "scalajs_template_engine",
        version := "01",
        scalaVersion := "2.12.8",

        scalaJSProjects := Seq(client),
        pipelineStages in Assets := Seq(scalaJSPipeline),
        compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,

        libraryDependencies ++= Seq(
            guice
            ,"org.glassfish.jaxb" % "jaxb-core" % "2.3.0.1"  // I have no idea what this does. Project doesn't run without this
            , "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.2" // I have no idea what this does. Project doesn't run without this
            , "org.apache.poi" % "poi" % "3.17"
            , "org.apache.poi" % "poi-ooxml" % "3.17"
            , "com.vmunier" %% "scalajs-scripts" % "1.1.2"
        ),
        JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

    )
    .dependsOn(sharedJVM)
    .enablePlugins(PlayScala)
    .aggregate(client,sharedJVM,sharedJS)


lazy val client = (project in file("client"))
    .settings(commonSettings)
    .settings(
        scalaJSUseMainModuleInitializer := false,
        jsDependencies += ProvidedJS / "customEvent-polyfill.js",
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % "0.9.7"
        )
    ).enablePlugins(ScalaJSWeb)
    .dependsOn(sharedJS)

lazy val sharedMacros = crossProject(JSPlatform,JVMPlatform)
    .crossType(CrossType.Pure)
    .settings(
        commonSettings,
        libraryDependencies ++= Seq(
            "org.scala-lang" % "scala-library" % "2.12.8"
            , "org.scala-lang" % "scala-reflect" % "2.12.8"
        )
    ).jvmSettings(
)
    .jsSettings(

    )

lazy val sharedMacrosJVM = sharedMacros.jvm
lazy val sharedMacrosJS = sharedMacros.js

lazy val shared = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .dependsOn(sharedMacros)
    .settings(commonSettings,
        libraryDependencies ++= Seq(
            "org.scala-lang.modules" %%% "scala-xml" % "1.2.0",
            "com.lihaoyi" %%% "upickle" % "0.7.5"
        )
    )
    .jvmSettings(
    )
    .jsSettings(

    )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val commonSettings = Seq(
    scalaVersion := "2.12.8"
)

onLoad in Global := (onLoad in Global).value.andThen(s => "project server" :: s)

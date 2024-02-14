ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "circe-yaml contributors"
ThisBuild / developers := List(
  tlGitHubDev("armanbilge", "Arman Bilge"),
  Developer("jeremyrsmith", "Jeremy Smith", "jeremyrsmith@gmail.com", url("https://github.com/jeremyrsmith")),
  Developer("jeffmay", "Jeff May", "jeff.n.may@gmail.com", url("https://github.com/jeffmay")),
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown"))
)
ThisBuild / startYear := Some(2022)

ThisBuild / tlSonatypeUseLegacyHost := false
ThisBuild / tlVersionIntroduced := List("2.13", "3").map(_ -> "0.0.3").toMap

val Versions = new {
  val circe = "0.14.5"
  val discipline = "1.5.1"
  val scalaCheck = "1.17.0"
  val scalaTest = "3.2.16"
  val scalaTestPlus = "3.2.14.0"
  val scalaYaml = "0.0.7"
}

ThisBuild / crossScalaVersions := List("2.13.10", "3.4.0")

ThisBuild / tlFatalWarningsInCi := false

lazy val root = tlCrossRootProject.aggregate(scalaYaml)

val scalaYaml = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("scala-yaml"))
  .settings(
    name := "circe-scala-yaml",
    description := "Library for converting between Scala Yaml's AST and circe's AST",
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13")
        List("-Ytasty-reader")
      else Nil
    },
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % Versions.circe,
      "org.virtuslab" %%% "scala-yaml" % Versions.scalaYaml,
      "io.circe" %%% "circe-jawn" % Versions.circe % Test,
      "io.circe" %%% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %%% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %%% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %%% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %%% "scalacheck-1-16" % Versions.scalaTestPlus % Test
    )
  )

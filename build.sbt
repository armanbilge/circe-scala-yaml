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

val Versions = new {
  val circe = "0.14.2"
  val discipline = "1.4.0"
  val scalaCheck = "1.15.4"
  val scalaTest = "3.2.12"
  val scalaTestPlus = "3.2.11.0"
  val snakeYaml = "1.28"
  val previousCirceYaml = "0.13.1"
}

ThisBuild / crossScalaVersions := List("3.1.3")

ThisBuild / tlFatalWarningsInCi := false

lazy val root = tlCrossRootProject.aggregate(scalaYaml)

val scalaYaml = crossProject(JVMPlatform, JSPlatform)
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
      "io.circe" %%% "circe-jawn" % Versions.circe % Test,
      "org.virtuslab" %%% "scala-yaml" % "0.0.4" cross CrossVersion.for2_13Use3,
      "io.circe" %%% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %%% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %%% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %%% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %%% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    )
  )

import sbt.Keys._
import sbt._

object Versions {
  val dotty      = "3.1.0"
  val cats       = "2.6.1"
  val catsEffect = "3.1.1"
  val munit      = "0.7.27"
  val aerospike  = "5.1.8"
}

object Settings {

  lazy val settings = Seq(
    scalacOptions ++= Seq("-new-syntax", "-rewrite")
  )

  lazy val cats        = Seq("org.typelevel" %% "cats-core").map(_ % Versions.cats)
  lazy val catsEffect  = Seq("org.typelevel" %% "cats-effect").map(_ % Versions.catsEffect)
  lazy val munit       = Seq("org.scalameta" %% "munit" % Versions.munit % Test)
  lazy val munitEffect = Seq("org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % "test")

  lazy val aerospike = Seq("com.aerospike" % "aerospike-client" % Versions.aerospike)
}

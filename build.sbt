import com.typesafe.tools.mima.core.*

Global / concurrentRestrictions += Tags.limit(Tags.Test, 2)

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

addCommandAlias(
  "testCoverage",
  "; clean ; coverage; test; coverageAggregate; coverageReport; coverageOff"
)

addCommandAlias(
  "styleFix",
  "; scalafmtSbt; scalafmtAll; headerCreateAll; scalafixAll"
)

addCommandAlias(
  "styleCheck",
  "; scalafmtCheckAll; headerCheckAll; scalafixAll --check"
)

Global / resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val root = project
    .in(file("."))
    .settings(basicSettings)
    .settings(
      publish / skip := true,
      mimaFailOnNoPrevious := false,
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start())
    )
    .aggregate(core, pekko, akka, akkaBusl, monix, zio)

lazy val basicSettings = Seq(
  organization := "net.nmoncho",
  description := "Helenus is collection of Scala utilities for Apache Cassandra",
  scalaVersion := Dependencies.Version.scala,
  startYear := Some(2021),
  homepage := Some(url("https://github.com/nMoncho/helenus")),
  licenses := Seq("MIT License" -> new URL("http://opensource.org/licenses/MIT")),
  headerLicense := Some(HeaderLicense.MIT("2021", "the original author or authors", HeaderLicenseStyle.SpdxSyntax)),
  developers := List(
    Developer(
      "nMoncho",
      "Gustavo De Micheli",
      "gustavo.demicheli@gmail.com",
      url("https://github.com/nMoncho")
    )
  ),
  scalacOptions := (Opts.compile.encoding("UTF-8") :+
      Opts.compile.deprecation :+
      Opts.compile.unchecked :+
      "-feature" :+
      "-release" :+
      "11" :+
      "-Wunused:all" :+
      "-Xcheck-macros" :+
      "-language:higherKinds"),
  (Test / testOptions) += Tests.Argument("-oF"),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val core = project
    .settings(basicSettings)
    .settings(
      name := "helenus-core",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaPreviousArtifacts := Set("net.nmoncho" %% "helenus-core" % "1.0.0"),
      mimaBinaryIssueFilters ++= Seq(
        ProblemFilters.exclude[DirectMissingMethodProblem](
          "net.nmoncho.helenus.api.type.codec.Codec.generateTupleCodec"
        ),
        ProblemFilters.exclude[DirectMissingMethodProblem](
          "net.nmoncho.helenus.internal.codec.TupleCodecDerivation.generateTupleCodec"
        )
      ),
      libraryDependencies ++= Seq(
        Dependencies.ossJavaDriver % Provided,
        Dependencies.slf4j,
        // Test Dependencies
        Dependencies.mockito       % Test,
        Dependencies.scalaCheck    % Test,
        Dependencies.scalaTest     % Test,
        Dependencies.scalaTestPlus % Test,
        Dependencies.logback       % Test,
        "net.java.dev.jna"         % "jna" % "5.17.0" % Test // Fixes M1 JNA issue
      )
    )

lazy val docs = project
    .in(file("helenus-docs"))
    .enablePlugins(MdocPlugin)
    .disablePlugins(ScoverageSbtPlugin)
    .settings(basicSettings)
    .settings(
      publish / skip := true,
      mdocVariables := Map(
        "VERSION" -> version.value
      ),
      mdocOut := file("."),
      libraryDependencies ++= Seq(
        Dependencies.ossJavaDriver,
        Dependencies.cassandraUnit
      )
    )
    .dependsOn(core)

lazy val akka = project
    .settings(basicSettings)
    .dependsOn(core % "compile->compile;test->test")
    .settings(
      name := "helenus-akka",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaPreviousArtifacts := Set("net.nmoncho" %% "helenus-akka" % "1.0.0"),
      // 5.x changed to business license
      dependencyUpdatesFilter -= moduleFilter(organization = "com.lightbend.akka"),
      // 2.7.x changed to business license
      dependencyUpdatesFilter -= moduleFilter(organization = "com.typesafe.akka"),
      libraryDependencies ++= Seq(
        Dependencies.alpakka.cross(CrossVersion.for3Use2_13)     % "provided,test",
        Dependencies.akkaTestKit.cross(CrossVersion.for3Use2_13) % Test,
        // Adding this until Alpakka aligns version with Akka TestKit
        ("com.typesafe.akka" %% "akka-stream" % Dependencies.Version.akka).cross(
          CrossVersion.for3Use2_13
        ) % "provided,test"
      )
    )

lazy val akkaBusl = project
    .in(file("akka-busl"))
    .settings(basicSettings)
    .dependsOn(core % "compile->compile;test->test")
    .settings(
      name := "helenus-akka-busl",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaPreviousArtifacts := Set("net.nmoncho" %% "helenus-akka-busl" % "1.0.0"),
      libraryDependencies ++= Seq(
        Dependencies.alpakkaBusl     % "provided,test",
        Dependencies.akkaTestKitBusl % Test,
        // Adding this until Alpakka aligns version with Akka TestKit
        "com.typesafe.akka" %% "akka-stream" % Dependencies.Version.akkaBusl % "provided,test"
      )
    )

lazy val monix = project
    .settings(basicSettings)
    .dependsOn(core % "compile->compile;test->test")
    .settings(
      name := "helenus-monix",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaFailOnNoPrevious := false,
      libraryDependencies ++= Seq(
        Dependencies.ossJavaDriver % Provided,
        Dependencies.monix         % "provided,test",
        Dependencies.monixReactive % "provided,test"
      )
    )

lazy val pekko = project
    .settings(basicSettings)
    .dependsOn(core % "compile->compile;test->test")
    .settings(
      name := "helenus-pekko",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaPreviousArtifacts := Set("net.nmoncho" %% "helenus-pekko" % "1.0.0"),
      libraryDependencies ++= Seq(
        Dependencies.pekkoConnector % "provided,test",
        Dependencies.pekkoTestKit   % Test,
        // Adding this until Alpakka aligns version with Pekko TestKit
        "org.apache.pekko" %% "pekko-stream" % Dependencies.Version.pekkoTestKit % "provided,test"
      )
    )

lazy val zio = project
    .settings(basicSettings)
    .dependsOn(core % "compile->compile;test->test")
    .settings(
      name := "helenus-zio",
      Test / testOptions += Tests.Setup(() => EmbeddedDatabase.start()),
      mimaFailOnNoPrevious := false,
      libraryDependencies ++= Seq(
        Dependencies.ossJavaDriver     % Provided,
        Dependencies.zio               % "provided,test",
        Dependencies.zioStreams        % "provided,test",
        Dependencies.zioStreamsInterop % "provided,test",
        Dependencies.zioTest           % Test,
        Dependencies.zioTestSbt        % Test,
        Dependencies.zioTestMagnolia   % Test
      )
    )

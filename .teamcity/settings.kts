import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.awsConnection
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubIssues
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.05"

project {

    buildType(Build)

    params {
        param("teamcity.internal.webhooks.BUILD_INTERRUPTED.fields", "fields=buildTypeId")
        param("teamcity.internal.webhooks.events", "BUILD_INTERRUPTED")
    }

    features {
        awsConnection {
            id = "AwsIamRoleMavenGHApp"
            name = "AWS-IAM-Role-MavenGHApp"
            credentialsType = iamRole {
                roleArn = "arn:aws:iam::913206223978:role/dk-EC2-role"
                awsConnectionId = "AwsDcpc"
            }
        }
        githubIssues {
            id = "PROJECT_EXT_17"
            displayName = "Valrravn/sample-java-app-maven"
            repositoryURL = "https://github.com/Valrravn/sample-java-app-maven"
            authType = storedToken {
                tokenId = "tc_token_id:CID_3083460fe032c30a4d92650323e03492:-1:c3665e56-8878-4dba-b226-170a2124938f"
            }
        }
    }
}

object Build : BuildType({
    name = "Build"

    allowExternalStatus = true

    params {
        param("release.status", "EAP")
    }

    vcs {
        root(DslContext.settingsRoot)

        checkoutMode = CheckoutMode.ON_SERVER
    }

    steps {
        script {
            name = "Show JVM -X parameters"
            scriptContent = "java -X"
        }
        maven {
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = bundled_3_8()
            jdkHome = "%env.JDK_11_0%"
            jvmArgs = "-verbose:gc -Xdiag -Xcomp -Xmn54m"
        }
    }

    triggers {
        vcs {
            branchFilter = """
                -:*
                +:pull/*
            """.trimIndent()
        }
    }

    features {
        perfmon {
        }
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
        pullRequests {
            provider = github {
                authType = vcsRoot()
                filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
            }
        }
        vcsLabeling {
            vcsRootId = "${DslContext.settingsRoot.id}"
            labelingPattern = "%release.status%"
        }
    }
})

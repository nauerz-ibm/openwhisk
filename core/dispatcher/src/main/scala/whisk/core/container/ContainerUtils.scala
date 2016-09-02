/*
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.container
//
//import whisk.common.Logging
//import whisk.common.SimpleExec
//import whisk.common.TransactionId
//import whisk.core.entity.ActionLimits
//import java.io.File
//import java.io.FileNotFoundException
//import scala.util.Try
//import scala.language.postfixOps
//import whisk.common.LoggingMarkers
//import akka.event.Logging.ErrorLevel
//import whisk.common.PrintStreamEmitter
//
///**
// * Information from docker ps.
// */
// case class ContainerState(id: ContainerHash, image: String, name: ContainerName)
//
//trait ContainerUtils extends Logging {
//
//    /** Defines the docker host, optional **/
//    val dockerhost: String
//
//    private def makeEnvVars(env: Map[String, String]): Array[String] = {
//        env.map {
//            kv => s"-e ${kv._1}=${kv._2}"
//        }.mkString(" ").split(" ").filter { x => x.nonEmpty }
//    }
//
//    /**
//     * Creates a container instance and runs it.
//     *
//     * @param image the docker image to run
//     * @return container id and container host
//     */
//    def bringup(name: Option[ContainerName], image: String, network: String, cpuShare: Int, env: Map[String, String], args: Array[String], limits: ActionLimits, policy: Option[String])(implicit transid: TransactionId): (ContainerHash, Option[ContainerAddr]) = {
//        val id = makeContainer(name, image, network, cpuShare, env, args, limits, policy)
//        val host = getContainerHostAndPort(id)
//        (id, host)
//    }
//
//    /**
//     * Pulls container images.
//     */
//    def pullImage(image: String)(implicit transid: TransactionId): DockerOutput = ContainerUtils.pullImage(dockerhost, image)
//
//    /*
//     * TODO: The file handle and process limits should be moved to some global limits config.
//     */
//    def makeContainer(name: Option[ContainerName], image: String, network: String, cpuShare: Int, env: Map[String, String], args: Seq[String], limits: ActionLimits, policy: Option[String])(implicit transid: TransactionId): ContainerHash = {
//        val nameOption = name.map(n => Array("--name", n.name)).getOrElse(Array.empty[String])
//        val cpuArg = Array("-c", cpuShare.toString)
//        val memoryArg = Array("-m", s"${limits.memory()}m")
//        val capabilityArg = Array("--cap-drop", "NET_RAW", "--cap-drop", "NET_ADMIN")
//        val consulServiceIgnore = Array("-e", "SERVICE_IGNORE=true")
//        val fileHandleLimit = Array("--ulimit", "nofile=64:64")
//        val processLimit = Array("--ulimit", "nproc=512:512")
//        val securityOpts = policy map { p => Array("--security-opt", s"apparmor:${p}") } getOrElse (Array.empty[String])
//        val containerNetwork = Array("--net", network)
//
//        val cmd = Seq("run") ++ makeEnvVars(env) ++ consulServiceIgnore ++ nameOption ++ cpuArg ++ memoryArg ++
//            capabilityArg ++ fileHandleLimit ++ processLimit ++ securityOpts ++ containerNetwork ++ Seq("-d", image) ++ args
//
//        runDockerCmd(cmd: _*).toOption.map { result =>
//            ContainerHash.fromString(result)
//        } getOrElse {
//            throw new Exception("Container hash or name expected in `makeContainer`.")
//        }
//    }
//
//    def killContainer(container: ContainerIdentifier)(implicit transid: TransactionId): DockerOutput = {
//        runDockerCmd("kill", container.id)
//    }
//
//    def getContainerLogs(container: ContainerIdentifier)(implicit transid: TransactionId): DockerOutput = {
//        runDockerCmd("logs", container.id)
//    }
//
//    def pauseContainer(container: ContainerIdentifier)(implicit transid: TransactionId): DockerOutput = {
//        runDockerCmd(true, Seq("pause", container.id))
//    }
//
//    def unpauseContainer(container: ContainerIdentifier)(implicit transid: TransactionId): DockerOutput = {
//        runDockerCmd(true, Seq("unpause", container.id))
//    }
//
//    /**
//     * Forcefully removes a container, can be used on a running container but not a paused one.
//     */
//    def rmContainer(container: ContainerIdentifier)(implicit transid: TransactionId): DockerOutput = {
//        runDockerCmd("rm", "-f", container.id)
//    }
//
//    /*
//     * List containers (-a if all).
//     */
//    def listContainers(all: Boolean)(implicit transid: TransactionId): Seq[ContainerState] = {
//        val tmp = Array("ps", "--no-trunc")
//        val cmd = if (all) tmp :+ "-a" else tmp
//        runDockerCmd(cmd: _*).toOption map { output =>
//            val lines = output.split("\n").drop(1).toSeq // skip the header
//            lines.map(parsePsOutput)
//        } getOrElse Seq()
//    }
//
//    def getDockerLogSize(containerId: ContainerHash, mounted: Boolean)(implicit transid: TransactionId): Long = {
//        try {
//            getDockerLogFile(containerId, mounted).length
//        } catch {
//            case e: Exception =>
//                error(this, s"getDockerLogSize failed on $containerId")
//                0
//        }
//    }
//
//    /**
//     * Reads the contents of the file at the given position.
//     * It is assumed that the contents does exist and that region is not changing concurrently.
//     */
//    def getDockerLogContent(containerHash: ContainerHash, start: Long, end: Long, mounted: Boolean)(implicit transid: TransactionId): Array[Byte] = {
//        var fis: java.io.FileInputStream = null
//        try {
//            val file = getDockerLogFile(containerHash, mounted)
//            fis = new java.io.FileInputStream(file)
//            val channel = fis.getChannel().position(start)
//            var remain = (end - start).toInt
//            val buffer = java.nio.ByteBuffer.allocate(remain)
//            while (remain > 0) {
//                val read = channel.read(buffer)
//                if (read > 0)
//                    remain = read - read.toInt
//            }
//            buffer.array
//        } catch {
//            case e: Exception =>
//                error(this, s"getDockerLogContent failed on ${containerHash.hash}: ${e.getClass}: ${e.getMessage}")
//                Array()
//        } finally {
//            if (fis != null) fis.close()
//        }
//
//    }
//
//    def getContainerHostAndPort(container: ContainerIdentifier)(implicit transid: TransactionId): Option[ContainerAddr] = {
//        // FIXME it would be good if this could return ContainerAddr and fail loudly instead.
//        runDockerCmd("inspect", "--format", "'{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'", container.id).toOption.map { output =>
//            ContainerAddr(output.substring(1, output.length - 1), 8080)
//        }
//    }
//
//    private def runDockerCmd(args: String*)(implicit transid: TransactionId): DockerOutput = runDockerCmd(false, args)
//
//    /**
//     * Synchronously runs the given docker command returning stdout if successful.
//     */
//    private def runDockerCmd(skipLogError: Boolean, args: Seq[String])(implicit transid: TransactionId): DockerOutput =
//        ContainerUtils.runDockerCmd(dockerhost, skipLogError, args)(transid)
//
//    // If running outside a container, then logs files are in docker's own
//    // /var/lib/docker/containers.  If running inside a container, is mounted at /containers.
//    // Root access is needed when running outside the container.
//    private def dockerContainerDir(mounted: Boolean) = {
//        if (mounted) "/containers" else "/var/lib/docker/containers"
//    }
//
//    /**
//     * Gets the filename of the docker logs of other containers that is mapped back into the invoker.
//     */
//    private def getDockerLogFile(containerId: ContainerHash, mounted: Boolean) = {
//        new java.io.File(s"""${dockerContainerDir(mounted)}/${containerId.hash}/${containerId.hash}-json.log""").getCanonicalFile()
//    }
//
//    private def parsePsOutput(line: String): ContainerState = {
//        val tokens = line.split("\\s+")
//        val hash = ContainerHash.fromString(tokens(0))
//        val name = ContainerName.fromString(tokens.last)
//        ContainerState(hash, tokens(1), name)
//    }
//}
//
//object ContainerUtils extends Logging {
//
//    private implicit val emitter: PrintStreamEmitter = this
//
//    /**
//     * Synchronously runs the given docker command returning stdout if successful.
//     */
//    def runDockerCmd(dockerhost: String, skipLogError: Boolean, args: Seq[String])(implicit transid: TransactionId): DockerOutput = {
//        val start = transid.started(this, LoggingMarkers.INVOKER_DOCKER_CMD(args(0)))
//
//        try {
//            val fullCmd = getDockerCmd(dockerhost) ++ args
//
//            val (stdout, stderr, exitCode) = SimpleExec.syncRunCmd(fullCmd)
//
//            if (exitCode == 0) {
//                transid.finished(this, start)
//                DockerOutput(stdout.trim)
//            } else {
//                if (!skipLogError) {
//                    transid.failed(this, start, s"stdout:\n$stdout\nstderr:\n$stderr", ErrorLevel)
//                } else {
//                    transid.failed(this, start)
//                }
//                DockerOutput.unavailable
//            }
//        } catch {
//            case t: Throwable =>
//                transid.failed(this, start, "error: " + t.getMessage, ErrorLevel)
//                DockerOutput.unavailable
//        }
//    }
//
//    private def getDockerCmd(dockerhost: String): Seq[String] = {
//        def file(path: String) = Try { new File(path) } filter { _.exists } toOption
//
//        val dockerLoc = file("/usr/bin/docker") orElse file("/usr/local/bin/docker")
//
//        val dockerBin = dockerLoc.map(_.toString).getOrElse {
//            throw new FileNotFoundException("Couldn't locate docker binary.")
//        }
//
//        if (dockerhost == "localhost") {
//            Seq(dockerBin)
//        } else {
//            Seq(dockerBin, "--host", s"tcp://$dockerhost")
//        }
//    }
//
//    /**
//     * Pulls container images.
//     */
//    def pullImage(dockerhost: String, image: String)(implicit transid: TransactionId): DockerOutput = {
//        val cmd = Array("pull", image)
//        runDockerCmd(dockerhost, false, cmd)
//    }
//
//}

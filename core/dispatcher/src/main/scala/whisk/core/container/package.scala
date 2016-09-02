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

package whisk.core

import scala.concurrent.duration._

import java.time.Instant

import whisk.core.entity.UUID
import whisk.core.entity.DocRevision

/**
 * This object contains type definitions that are useful when observing and timing container operations.
 */
package object container {

    /**
     * Identifies a combination of owner+action+version (except special cases)
     */
    class ActionContainerId(val stringRepr: String) extends AnyVal

    object ActionContainerId {
        // Convenience "constructor" since this is the most common case.
        def apply(uuid: UUID, actionFullyQualifiedName: String, actionRevision: DocRevision) =
            new ActionContainerId(s"instantiated.${uuid}.${actionFullyQualifiedName}.${actionRevision}")
    }

    /**
     * Special case for stem cell containers
     */
    val WarmNodeJsActionContainerId = new ActionContainerId("warm.nodejs")

    /**
     * Represents a time interval, which can be viewed as a duration for which
     *  the start/end instants are fully known (as opposed to being relative).
     */
    case class Interval(start: Instant, end: Instant) {
        def duration = Duration.create(end.toEpochMilli() - start.toEpochMilli(), MILLISECONDS)
    }

    /**
     * Represents the result of accessing an endpoint in a container:
     * Start time, End time, Some(response) from container consisting of status code and payload
     * If there is no response or an exception, then None.
     */
    case class RunResult(interval: Interval, response: Option[(Int, String)]) {
        def duration = interval.duration
    }

    /**
     * The result of trying to obtain a container.
     */
    sealed trait ContainerResult

    case object CacheMiss extends ContainerResult

    /**
     * The result of trying to obtain a container which is known to exist.
     */
    sealed trait FinalContainerResult extends ContainerResult

    case class Success(con: Container, initResult: Option[RunResult]) extends FinalContainerResult
    case object Busy extends FinalContainerResult
    case class Error(string: String) extends FinalContainerResult

    // Note: not using InetAddress here because we don't want to do any lookup
    // until used for something.
    case class ContainerAddr(host: String, port: Int) {
        override def toString() = s"$host:$port"
    }

    sealed abstract class ContainerIdentifier(val id: String)
    class ContainerName(val name: String) extends ContainerIdentifier(name)
    class ContainerHash(val hash: String) extends ContainerIdentifier(hash)

    object ContainerIdentifier {
        def fromString(str: String): ContainerIdentifier = {
            val s = str.trim
            require(!s.contains("\n"))
            if (s.matches("^[0-9a-fA-F]+$")) {
                new ContainerHash(s)
            } else {
                new ContainerName(s)
            }
        }
    }

    object ContainerName {
        def fromString(str: String) = new ContainerName(str)
    }

    object ContainerHash {
        def fromString(str: String) = {
            require(str.matches("^[0-9a-fA-F]+$"))
            new ContainerHash(str)
        }
    }

    /**
     * Information from docker ps.
     */
    case class ContainerState(id: ContainerHash, image: String, name: ContainerName)

    final class DockerOutput(val toOption: Option[String]) extends AnyVal
    object DockerOutput {
        def apply(content: String) = new DockerOutput(Some(content))
        def unavailable = new DockerOutput(None)
    }
}

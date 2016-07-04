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
package whisk.core.controller.v2

import akka.http._
import akka.http.scaladsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import whisk.common.TransactionId
import whisk.core.entity.WhiskActivation

import spray.json._

trait Activations extends AuthenticatedRoute {

    def activationRoutes = (
        activationListRoute
    )

    val activationListRoute =
        basicAuth(TransactionId.unknown) { auth =>
            path("api" / "v2" / "namespace" / "_" / "activations") {
                get {
                    complete(JsObject("error" -> JsBoolean(true)))
                }
            }
        }

}

/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.booleanValue
import com.vaticle.typedb.iam.simulation.typedb.Util.datetimeValue
import com.vaticle.typedb.iam.simulation.typedb.Util.longValue
import java.time.LocalDateTime

data class TypeDBPermission(val permittedSubject: TypeDBSubject, val permittedAccess: TypeDBAccess, val validity: Boolean, val reviewDate: LocalDateTime) {
    constructor(permittedSubject: TypeDBSubject, permittedAccess: TypeDBAccess, validity: Concept, reviewDate: Concept):
            this(permittedSubject, permittedAccess, booleanValue(validity), datetimeValue(reviewDate))
}
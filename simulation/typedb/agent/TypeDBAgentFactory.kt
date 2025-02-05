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
package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.iam.simulation.agent.*
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.simulation.typedb.TypeDBClient

class TypeDBAgentFactory(client: TypeDBClient, context: Context): AgentFactory<TypeDBClient>(client, context) {
    override fun user(client: TypeDBClient, context: Context) = TypeDBUserAgent(client, context)
    override fun owner(client: TypeDBClient, context: Context) = TypeDBOwnerAgent(client, context)
    override fun supervisor(client: TypeDBClient, context: Context) = TypeDBSupervisorAgent(client, context)
    override fun policyManager(client: TypeDBClient, context: Context) = TypeDBPolicyManagerAgent(client, context)
    override fun sysAdmin(client: TypeDBClient, context: Context) = TypeDBSysAdminAgent(client, context)
}

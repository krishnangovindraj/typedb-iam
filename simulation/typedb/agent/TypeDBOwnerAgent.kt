package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.OwnerAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import kotlin.streams.toList

class TypeDBOwnerAgent(client: TypeDBClient, context:Context): OwnerAgent<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun changeGroupOwnership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP)?.asSubject() ?: return listOf()
        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(S).isa(group.type).has(group.idType, group.idValue).has(PARENT_COMPANY_NAME, company.name),
                        cvar(S_OWNER).isa(owner.type).has(owner.idType, owner.idValue).has(PARENT_COMPANY_NAME, company.name),
                        rel(OWNED_GROUP, S).rel(GROUP_OWNER, S_OWNER).isa(GROUP_OWNERSHIP)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        session.transaction(WRITE).use { tx ->
            tx.query().delete(
                match(
                    cvar(S).isa(group.type).has(group.idType, group.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(OW).rel(OWNED_GROUP, S).isa(GROUP_OWNERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(OW).isa(GROUP_OWNERSHIP),
                )
            )

            tx.query().insert(
                match(
                    cvar(S).isa(group.type).has(group.idType, group.idValue),
                    cvar(S_OWNER).isa(owner.type).has(owner.idType, owner.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_OWNER).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    rel(OWNED_GROUP, S).rel(GROUP_OWNER, S_OWNER).isa(GROUP_OWNERSHIP),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    override fun changeObjectOwnership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val obj = getRandomEntity(session, company, randomSource, OBJECT)?.asObject() ?: return listOf()
        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue).has(PARENT_COMPANY_NAME, company.name),
                        cvar(S).isa(owner.type).has(owner.idType, owner.idValue).has(PARENT_COMPANY_NAME, company.name),
                        rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        session.transaction(WRITE).use { tx ->
            tx.query().delete(
                match(
                    cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(OW).rel(OWNED_OBJECT, O).isa(OBJECT_OWNERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(OW).isa(OBJECT_OWNERSHIP),
                )
            )

            tx.query().insert(
                match(
                    cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                    cvar(O_OWNER).isa(owner.type).has(owner.idType, owner.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O_OWNER).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, O_OWNER).isa(OBJECT_OWNERSHIP),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    companion object {
        private const val C = "c"
        private const val O = "o"
        private const val OW = "ow"
        private const val O_OWNER = "o-owner"
        private const val S = "s"
        private const val S_OWNER = "s-owner"
    }
}

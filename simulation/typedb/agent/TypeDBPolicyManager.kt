package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.PolicyManager
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESSED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMISSION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REVIEW_DATE
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATED_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATION_POLICY
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALIDITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATING_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATED_POLICY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATING_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Proof
import com.vaticle.typedb.iam.simulation.typedb.Util.getProofs
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.concept.*
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import kotlin.streams.toList

class TypeDBPolicyManager(client: TypeDBClient, context:Context): PolicyManager<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)
    override fun listPermissionsPendingReview(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(VALIDITY, P_VALIDITY)
                        .has(REVIEW_DATE, P_DATE),
                    `var`(P_DATE).lte(context.model.permissionReviewAge.toLong()),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        return listOf<Report>()
    }

    override fun reviewPermissions(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(VALIDITY, P_VALIDITY)
                        .has(REVIEW_DATE, P_DATE),
                    `var`(P_DATE).lte(context.model.permissionReviewAge.toLong()),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        permissions.forEach { permission ->
            val permittedSubject = permission.permittedSubject
            val accessedObject = permission.permittedAccess.accessedObject
            val validAction = permission.permittedAccess.validAction

            session.transaction(WRITE, options).use { transaction ->
                transaction.query().delete(
                    match(
                        `var`(S).isa(permittedSubject.type)
                            .has(permittedSubject.idType, permittedSubject.idValue),
                        `var`(O).isa(accessedObject.type)
                            .has(accessedObject.idType, accessedObject.idValue),
                        `var`(A).isa(validAction.type)
                            .has(validAction.idType, validAction.idValue),
                        `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                        `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                    ).delete(
                        `var`(P).isa(PERMISSION)
                    )
                )

                if (randomSource.nextInt(100) < context.model.permissionRenewalPercentage) {
                    transaction.query().insert(
                        match(
                            `var`(S).isa(permittedSubject.type)
                                .has(permittedSubject.idType, permittedSubject.idValue),
                            `var`(O).isa(accessedObject.type)
                                .has(accessedObject.idType, accessedObject.idValue),
                            `var`(A).isa(validAction.type)
                                .has(validAction.idType, validAction.idValue),
                            `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                            `var`(C).isa(COMPANY)
                                .has(NAME, company.name),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                        ).insert(
                            rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                                .has(REVIEW_DATE, (context.iterationNumber + context.model.permissionReviewAge).toLong())
                        )
                    )
                }

                transaction.commit()
            }
        }

        return listOf<Report>()
    }

    override fun assignSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val action1 = getRandomEntity(session, company, randomSource, OPERATION).asAction()
        val action2 = getRandomEntity(session, company, randomSource, OPERATION).asAction()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(A1).isa(action1.type)
                        .has(action1.idType, action1.idValue),
                    `var`(A2).isa(action2.type)
                        .has(action2.idType, action2.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A1).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A2).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY)
                )
            )
        }

        return listOf<Report>()
    }

    override fun listSegregationPolicies(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<SegregationPolicy>

        session.transaction(READ, options).use { transaction ->
            segregationPolicies = transaction.query().match(
                match(
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY),
                    `var`(A1).isaX(A1_TYPE),
                    `var`(A2).isaX(A2_TYPE)
                )
            ).toList().map {
                SegregationPolicy(
                    Action(it[A1_TYPE], it[A1_NAME]),
                    Action(it[A1_TYPE], it[A1_NAME])
                )
            }
        }

        return listOf<Report>()
    }

    override fun revokeSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<SegregationPolicy>

        session.transaction(READ, options).use { transaction ->
            segregationPolicies = transaction.query().match(
                match(
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY),
                    `var`(A1).isaX(A1_TYPE),
                    `var`(A2).isaX(A2_TYPE)
                )
            ).toList().map {
                SegregationPolicy(
                    Action(it[A1_TYPE], it[A1_NAME]),
                    Action(it[A1_TYPE], it[A1_NAME])
                )
            }
        }

        val segregationPolicy = randomSource.choose(segregationPolicies)
        val action1 = segregationPolicy.action1
        val action2 = segregationPolicy.action2

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(A1).isa(action1.type)
                        .has(action1.idType, action1.idValue),
                    `var`(A2).isa(action2.type)
                        .has(action2.idType, action2.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A1).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A2).isa(COMPANY_MEMBERSHIP),
                    `var`(SP).rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY)
                ).delete(
                    `var`(SP).isa(SEGREGATION_POLICY)
                )
            )
        }

        return listOf<Report>()
    }

    override fun listSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationViolations: List<SegregationViolation>

        session.transaction(READ, options).use { transaction ->
            segregationViolations = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    `var`(SP).rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY),
                    rel(VIOLATING_SUBJECT, S).rel(VIOLATING_OBJECT, O).rel(VIOLATED_POLICY).isa(SEGREGATION_POLICY),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE)
                )
            ).toList().map {
                SegregationViolation(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                    SegregationPolicy(
                        Action(it[A1_TYPE], it[A1_NAME]),
                        Action(it[A2_TYPE], it[A2_NAME])
                    )
                )
            }
        }

        return listOf<Report>()
    }

    override fun reviewSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationViolations: Map<SegregationViolation, Set<Proof>>

        session.transaction(READ, options).use { transaction ->
            segregationViolations = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    `var`(SP).rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY),
                    rel(VIOLATING_SUBJECT, S).rel(VIOLATING_OBJECT, O).rel(VIOLATED_POLICY).isa(SEGREGATION_POLICY),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE)
                )
            ).toList().map {
                SegregationViolation(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                    SegregationPolicy(
                        Action(it[A1_TYPE], it[A1_NAME]),
                        Action(it[A2_TYPE], it[A2_NAME])
                    )
                ) to getProofs(transaction, it)
            }.toMap()
        }

        return listOf<Report>()
    }

    companion object {
        private const val A = "a"
        private const val A1 = "a1"
        private const val A1_NAME = "a1-name"
        private const val A1_TYPE = "a1-type"
        private const val A2 = "a2"
        private const val A2_NAME = "a2-name"
        private const val A2_TYPE = "a2-type"
        private const val AC = "ac"
        private const val A_NAME = "a-name"
        private const val A_TYPE = "a-type"
        private const val C = "c"
        private const val O = "o"
        private const val O_ID = "o-id"
        private const val O_ID_TYPE = "o-id-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val P_DATE = "p-date"
        private const val P_VALIDITY = "p-validity"
        private const val S = "s"
        private const val SP = "sp"
        private const val S_ID = "s-id"
        private const val S_ID_TYPE = "s-id-type"
        private const val S_TYPE = "s-type"
    }
}

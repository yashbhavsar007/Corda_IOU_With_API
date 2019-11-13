package com.template.states

import com.template.contracts.IOUContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.util.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.crypto.NullKeys

// *********
// * State *
// *********
@BelongsToContract(IOUContract::class)
data class IOUState(val amount : Amount<Currency>,
                    val issuer : AbstractParty,
                    override val owner : AbstractParty) : OwnableState {

    override val participants get() = listOf(issuer,owner)
    //fun withoutOwner() = copy(owner = AnonymousParty(NullKeys.NullPublicKey))
    override fun withNewOwner( newOwner: AbstractParty) = CommandAndState(IOUContract.Commands.Transfer(), copy(owner = newOwner, issuer = issuer, amount = amount))
    fun withTransfer(Newamount : Amount<Currency>) = CommandAndState(IOUContract.Commands.Transfer(), copy( amount = amount - Newamount , issuer = issuer, owner = owner))
    fun withtrx(Newamount : Amount<Currency> , newOwner: AbstractParty ) = CommandAndState(IOUContract.Commands.Transfer(), copy( amount = Newamount , issuer = issuer, owner = newOwner))
}

package com.template.webserver

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import net.corda.core.identity.CordaX500Name
import net.corda.core.contracts.Amount
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest
import net.corda.core.messaging.startTrackedFlow
import com.template.flows.IOUIssueFlow
import net.corda.core.utilities.getOrThrow
import java.util.*
import net.corda.core.contracts.ContractState
import net.corda.core.node.services.vault.*
import net.corda.core.messaging.RPCReturnsObservables
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.Vault.Page
import com.template.states.IOUState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import java.util.HashSet
import net.corda.core.contracts.LinearState
import java.util.Arrays.asList
import java.util.Collections.singletonList
import java.util.Collections.singletonList






/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @GetMapping(value = "/test", produces = arrayOf("text/plain "))
    private  fun test():String{
        return "TESTING HERE"
    }

    @PostMapping(value = "/issue", produces = ["application/json"],  headers = ["Content-Type=application/json"])
    private fun CallingFlow(request:HttpServletRequest):ResponseEntity<String>{
        val amount = request.getParameter("amount")
        val am = Amount.parseCurrency("$amount USD")
        val pname = request.getParameter("partyName")

        if(pname == null){
            return ResponseEntity.badRequest().body("Query parameter 'partyName' must not be null.\n")
        }
        if(amount.toLong() <= 0 ){
            return ResponseEntity.badRequest().body("Query parameter 'amount' must be greater than 0.\n")
        }

        val partyX500Name = CordaX500Name.parse(pname)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $pname cannot be found.\n")

        // return ResponseEntity.status(HttpStatus.CREATED).body("PartyName is ${request.getParameterNames()} Amount is ${request.getParameter("partyName")} and  ${Amount.parseCurrency("10 USD")} .\n")
        return try{
            val signedTx = proxy.startTrackedFlow(::IOUIssueFlow, am,otherParty).returnValue.getOrThrow()

            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex:Throwable) {

            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }



    @PostMapping(value= "getState", produces = ["application/json"], headers = ["Content-Type=application/json"])
    private fun GetState(request: HttpServletRequest):ResponseEntity<String>{

//        val dummyStateRef = StateRef(SecureHash.zeroHash, 0)
//        val contractStateTypes = HashSet(listOf(IOUState::class.java))
//
//        val queryByStateRefCriteria = VaultQueryCriteria(Vault.StateStatus.UNCONSUMED,contractStateTypes)
//
//        val sortAttribute = SortAttribute.Standard(Sort.VaultStateAttribute.CONSTRAINT_TYPE)


       // val contractStateTypes = HashSet(listOf<Class<IOUState>>(Cash.State::class.java))

        val queryByStateRefResults = proxy.vaultQuery<ContractState>(IOUState::class.java)
                //vaultQueryBy(queryByStateRefCriteria, PageSpecification(DEFAULT_PAGE_SIZE),Sort(setOf(Sort.SortColumn(sortAttribute, Sort.Direction.ASC))),IOUState::class.java)
      //  val queryByStateRefMetadata = queryByStateRefResults.statesMetadata

        return ResponseEntity.status(HttpStatus.CREATED).body( "{data: $queryByStateRefResults}" )
        //("contractStateTypes is $contractStateTypes queryByStateRefCriteria is $queryByStateRefCriteria  sortAttribute is $sortAttribute queryByStateRefMetadata is $queryByStateRefMetadata  queryByStateRefResults is $queryByStateRefResults and ${queryByStateRefResults.states} ")


    }
}
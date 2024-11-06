package com.orbitalhq.cockpit.core.query

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.readValues
import com.orbitalhq.playground.OperationStub
import com.orbitalhq.playground.StubQueryMessage
import com.orbitalhq.playground.StubQueryService
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.stubbing.StubService
import com.orbitalhq.utils.Ids
import org.reactivestreams.Publisher
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * This is the cockpit facing api controller for the Stub Query servce,
 * allowing cockpit users to submit stubbed queries, similar to Taxi playground
 */
@RestController
class StubQueryApi(private val schemaProvider: SchemaProvider) {

   /**
    * Accepts a StubQueryMessage, and executes the query.
    * Note that this does not use the schema passed on the message (and you should
    * pass an empty string).
    *
    * Instead, it uses the current schema
    */
   @PostMapping("/api/query/stub")
   fun submitQueryWithStub(@RequestBody message: StubQueryMessage): Mono<ResponseEntity<Publisher<Any>>> {
      val (vyne, stub) = StubService.stubbedVyne(schemaProvider.schema)
      val stubService = StubQueryService()
      val queryId: String = Ids.id(prefix = "query-", size = 12)



      val (queryResult, contentType) = stubService.submitQuery(
         vyne, stub, doMetroHack(message), queryId
      )
      val publisher =
         when (queryResult) {
            is Mono<*> -> queryResult.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
            is Flux<*> -> queryResult.onErrorMap { ResponseStatusException(HttpStatus.BAD_REQUEST, it.message) }
            else -> error("Unknown type of publisher: ${queryResult::class.simpleName}")
         } as Publisher<Any>
      return Mono.just(
         ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header("x-query-id", queryId)
            .body(publisher)

      )
   }

   private fun doMetroHack(message: StubQueryMessage): StubQueryMessage {
      val outerMessage = jacksonObjectMapper().readValue<Map<String,Any>>("""{
   "AccountId": "10929927",
   "partition": 4,
   "offset": 230351,
   "topic": "dev.payments.sdi.account",
   "opTs": "2024-11-06 12:19:48.000"
}""").toMutableMap()
      outerMessage["Account"] = avroMessage
      val json = jacksonObjectMapper().writeValueAsString(listOf(outerMessage))
      val updated = message.copy(
         stubs = message.stubs + listOf(OperationStub(
            "accounts", json
         ))
      )
      return updated

   }

}

val avroMessage = """
 {
      "UNC_PATH": null,
      "PAPERLESS_STMT": "GB09MYMB23058010929927",
      "CARD_TYPE": null,
      "CARD_NUMBER": null,
      "CARD_NAME": null,
      "CHEQUE_BOOK": null,
      "DEPOSIT_TYPE": null,
      "INITIAL_DEPOSIT": null,
      "OTHER_BANK_ACCT": null,
      "OTHER_BANK_NAME": null,
      "OTHER_BANK_ADDR": null,
      "OTHER_BANK_POST": null,
      "KEY_FACT_REVIEW": null,
      "ACCT_PURPOSE": null,
      "SIGN_AUTHORITY": null,
      "MB_INCENTIVE": null,
      "OTHER_CARD_BAL": null,
      "OTH_CD_AVIL_BAL": null,
      "OTH_CD_STR_DATE": null,
      "OTH_CD_TRF_REQD": null,
      "AUTO_CARD_REPAY": null,
      "INT_BANK_ACCT": null,
      "INT_SORT_CODE": null,
      "INT_BANK_NAME": null,
      "INT_BANK_ADDR": null,
      "INT_BANK_POST": null,
      "OTHER_SORT_CODE": null,
      "FUL_LEAF_CNT": null,
      "WIN_LEAF_CNT": null,
      "FUL_CHQ_BOOK": null,
      "WIN_CHQ_BOOK": null,
      "IBAN_NUMBER": "GB09MYMB23058010929927",
      "OTHR_YR_SUB_AMT": null,
      "OTHER_YEAR_INT": null,
      "ISA_CHRGS": null,
      "DOC_FORM_RECVD": null,
      "RE_VERIF_DATE": null,
      "RE_VERIF_DETAIL": null,
      "ISA_ATTORN_NAME": null,
      "NI_NUMBER": null,
      "ISA_MANDATE": null,
      "VALID_CUST_APPL": null,
      "VALID_CUST_SUBS": null,
      "DATE_OF_BIRTH": null,
      "ISA_PROD_TYPE": null,
      "ISA_RATE": null,
      "ISA_INT_TODAY": null,
      "ISA_STATUS": null,
      "ISA_MAX_SUB": null,
      "CURR_YR_END_DAT": null,
      "PREV_YR_END_DAT": null,
      "CURR_YR_SUB_AMT": null,
      "PREV_YR_SUB_AMT": null,
      "PREV_YR_SUB_INT": null,
      "AOS_PRINT_DT": null,
      "AOS_REPRINT": null,
      "TRANS_ID": null,
      "DECISION": null,
      "CUST_REF_OS": null,
      "LAST_CRED_CHECK": null,
      "LIMIT_TIME_STP": null,
      "KYC_STATUS": null,
      "AML_STATUS": null,
      "TIME_STAMP_KYC": null,
      "TIME_STAMP_AML": null,
      "MAN_OVERRIDE": null,
      "MAN_OVER_REASON": null,
      "MTR_LIMIT_CCY": null,
      "MTR_LOA_PUR": null,
      "CARD_HLD_NAME": null,
      "MTR_DO_CC_CHECK": null,
      "MTR_OVE_LMT": null,
      "JOIN_HLD_CARD": null,
      "ISA_CUST_RESID": null,
      "CONT_NOSTRO_AC": null,
      "R85": null,
      "R85_DATE_ADDED": null,
      "R85_DATE_REMOVED": null,
      "R105": null,
      "R105_DATE_ADDED": null,
      "R105_DATE_REMOVED": null,
      "FICO_GLOBAL": null,
      "FICO_OD_MAS": null,
      "FICO_CC_MAS": null,
      "FICO_PL_MAS": null,
      "PROD_INT_AMT": null,
      "PROD_ADV_AMT": null,
      "METR_OVRDUE_CTR": null,
      "METR_STAT_FLAG": null,
      "MTR_PRINT_FLAG": null,
      "OVERDUE_STAT": null,
      "OVERDUE_DAYS": null,
      "DATE_OF_DEATH": null,
      "AC_GEN_COND": null,
      "ISA_TERM": null,
      "MATURITY_DATE": null,
      "PREV_FIXED_ISA": null,
      "LAST_MOD_DATE": null,
      "ISA_ROLLOVER": null,
      "RAG_STATUS_USER": null,
      "PROV_COMMENT": null,
      "PROV_DTE_COMT": null,
      "MB_COMMENT": null,
      "RAG_STATUS_SYS": null,
      "ISA_ALLOW_REM": null,
      "ACCT_NICKNAME": null,
      "CSC_PROCESS": null,
      "METR_IB_ACCOUNT": null,
      "RAG_DATE": null,
      "PROCESS_STAGE": null,
      "PREV_MAT": null,
      "BCUR_AMT_WAIVE": null,
      "NBA_FREQUENCY": null,
      "MET_PRICARD_HOL": null,
      "MET_CO_APP": null,
      "FICO_MAX_LMT": null,
      "FICO_LMT_STATUS": null,
      "LOPS_MAX_LMT": null,
      "ACCT_FSCS_ELIG": "NA",
      "OPENING_CHANNEL": null,
      "CY_FRST_SUBDATE": null,
      "L_BB_SEQ_NO": null,
      "MG_DEPENDENTS": null,
      "EXTERNAL_ID": null,
      "FP_ALLOWED": null,
      "FP_REJECT_CODE": null,
      "EXTREF_ID": null,
      "COP_OPT_OUT": null,
      "ACCOUNT_NUMBER": null,
      "CUSTOMER": "10086450",
      "CATEGORY": "3102",
      "MNEMONIC": null,
      "POSITION_TYPE": "TR",
      "CURRENCY": "GBP",
      "CURRENCY_MARKET": "1",
      "LIMIT_REF": null,
      "ACCOUNT_OFFICER": "1",
      "OTHER_OFFICER": null,
      "RECONCILE_ACCT": null,
      "INTEREST_LIQU_ACCT": null,
      "INTEREST_COMP_ACCT": null,
      "INT_NO_BOOKING": null,
      "REFERAL_CODE": null,
      "WAIVE_LEDGER_FEE": null,
      "CONDITION_GROUP": null,
      "INACTIV_MARKER": null,
      "OPEN_ACTUAL_BAL": null,
      "OPEN_CLEARED_BAL": null,
      "ONLINE_ACTUAL_BAL": null,
      "ONLINE_CLEARED_BAL": null,
      "WORKING_BALANCE": null,
      "DATE_LAST_CR_CUST": null,
      "AMNT_LAST_CR_CUST": null,
      "TRAN_LAST_CR_CUST": null,
      "DATE_LAST_CR_AUTO": null,
      "AMNT_LAST_CR_AUTO": null,
      "TRAN_LAST_CR_AUTO": null,
      "DATE_LAST_CR_BANK": null,
      "AMNT_LAST_CR_BANK": null,
      "TRAN_LAST_CR_BANK": null,
      "DATE_LAST_DR_CUST": null,
      "AMNT_LAST_DR_CUST": null,
      "TRAN_LAST_DR_CUST": null,
      "DATE_LAST_DR_AUTO": null,
      "AMNT_LAST_DR_AUTO": null,
      "TRAN_LAST_DR_AUTO": null,
      "DATE_LAST_DR_BANK": null,
      "AMNT_LAST_DR_BANK": null,
      "TRAN_LAST_DR_BANK": null,
      "CAP_DATE_CHARGE": [
         {
            "seq": "1",
            "value": "20190228"
         }
      ],
      "CAP_DATE_CR_INT": null,
      "CAP_DATE_C2_INT": null,
      "CAP_DATE_DR_INT": null,
      "CAP_DATE_D2_INT": null,
      "CAP_BACK_VALUE": null,
      "ACCR_CHG_CATEG": null,
      "ACCR_CHG_TRANS": null,
      "ACCR_CHG_AMOUNT": null,
      "ACCR_CHG_SUSP": null,
      "ACCR_CR_CATEG": null,
      "ACCR_CR_TRANS": null,
      "ACCR_CR_AMOUNT": null,
      "ACCR_CR_SUSP": null,
      "ACCR_CR2_CATEG": null,
      "ACCR_CR2_TRANS": null,
      "ACCR_CR2_AMOUNT": null,
      "ACCR_CR2_SUSP": null,
      "ACCR_DR_CATEG": null,
      "ACCR_DR_TRANS": null,
      "ACCR_DR_AMOUNT": null,
      "ACCR_DR_SUSP": null,
      "ACCR_DR2_CATEG": null,
      "ACCR_DR2_TRANS": null,
      "ACCR_DR2_AMOUNT": null,
      "ACCR_DR2_SUSP": null,
      "CONSOL_KEY": null,
      "INT_LIQU_TYPE": null,
      "INT_LIQU_ACCT": null,
      "INT_LIQ_CCY": null,
      "PASSBOOK": "NO",
      "START_YEAR_BAL": null,
      "OPENING_DATE": "20120413",
      "VALUE_DATE": null,
      "CREDIT_MOVEMENT": null,
      "DEBIT_MOVEMENT": null,
      "VALUE_DATED_BAL": null,
      "CONTINGENT_BAL_CR": null,
      "CONTINGENT_BAL_DR": null,
      "OPEN_CATEGORY": "3102",
      "OPEN_VAL_DATED_BAL": null,
      "LINK_TO_LIMIT": null,
      "CLOSURE_DATE": null,
      "LOCKED_WITH_LIMIT": null,
      "CHARGE_ACCOUNT": null,
      "CHARGE_CCY": "GBP",
      "CHARGE_MKT": "1",
      "INTEREST_CCY": "GBP",
      "INTEREST_MKT": "1",
      "CON_CHARGE_ACCR": null,
      "CON_INTEREST_ACCR": null,
      "ALT_ACCT_TYPE": [
         {
            "seq": "1",
            "value": "LEGACY"
         }
      ],
      "ALT_ACCT_ID": null,
      "PREMIUM_TYPE": null,
      "CAP_DATE_PRM": null,
      "PREMIUM_FREQ": null,
      "APR": null,
      "ALLOW_NETTING": "NO",
      "LEDG_RECO_WITH": null,
      "STMT_RECO_WITH": null,
      "OUR_EXT_ACCT_NO": null,
      "RECO_TOLERANCE": null,
      "STOCK_CONTROL_TYPE": null,
      "SERIAL_NO_FORMAT": null,
      "AUTO_PAY_ACCT": null,
      "ORIG_CCY_PAYMENT": null,
      "AUTO_REC_CCY": null,
      "ORIGINAL_ACCT": null,
      "DISPO_OFFICER": null,
      "DISPO_EXEMPT": null,
      "TAX_SUSPEND": null,
      "TAX_AT_SETTLE": null,
      "ICA_MAIN_ACCOUNT": null,
      "ICA_DISTRIB_RATIO": null,
      "ICA_MAIN_ACCT_IND": null,
      "ICA_DISTRIB_TYPE": null,
      "ICA_POST_INTEREST": null,
      "ICA_MAIN_RATIO": null,
      "ICA_NEW_MAIN_ACC": null,
      "ICA_START_DATE": null,
      "ICA_ADD_REMOVE": null,
      "ICA_BACK_VALUE": null,
      "ICA_MAIN_ACCT": null,
      "ICA_MAIN_DATE": null,
      "LIQUIDATION_MODE": null,
      "OVERDUE_STATUS": "CUR",
      "HVT_FLAG": null,
      "SINGLE_LIMIT": null,
      "CONTINGENT_INT": null,
      "ALL_IN_ONE_PRODUCT": null,
      "ER_VALUE_DATE": null,
      "ER_BALANCE": null,
      "EP_BALANCE": null,
      "SB_GROUP_ID": null,
      "OPEN_AVAILABLE_BAL": null,
      "AV_NAU_DB_MVMT": null,
      "AV_AUTH_CR_MVMT": null,
      "AV_NAU_CR_MVMT": null,
      "FORWARD_MVMTS": null,
      "CREDIT_CHECK": null,
      "AVAILABLE_BAL_UPD": null,
      "CONSOLIDATE_ENT": null,
      "MAX_SUB_ACCOUNT": null,
      "MASTER_ACCOUNT": null,
      "LOCK_INC_THIS_MVMT": null,
      "CLOSED_ONLINE": null,
      "NEXT_AF_DATE": null,
      "NEXT_ACCT_CAP": null,
      "NEXT_EXP_DATE": null,
      "DATE_LAST_UPDATE": null,
      "NEXT_STMT_DATE": null,
      "EXPOSURE_DATES": null,
      "PORTFOLIO_NO": null,
      "SHADOW_ACCOUNT": null,
      "FWD_ENTRY_HOLD": null,
      "FIRST_AF_DATE": null,
      "CASH_POOL_GROUP": null,
      "OPEN_ASSET_TYPE": null,
      "LAST_COM_CHG_DATE": null,
      "IC_CHARGE_ID": null,
      "IC_NEXT_CAP_DATE": null,
      "IC_PRODUCT": null,
      "IC_LST_PROD_CAP": null,
      "ARRANGEMENT_ID": "AA12104C1HS1",
      "ACC_DEB_LIMIT": null,
      "MANDATE_APPL": null,
      "MANDATE_REG": null,
      "MANDATE_RECORD": null,
      "DR_ADJ_AMOUNT": null,
      "DR2_ADJ_AMOUNT": null,
      "CR_ADJ_AMOUNT": null,
      "CR2_ADJ_AMOUNT": null,
      "ACCOUNTING_COMPANY": null,
      "CLOSURE_REASON": null,
      "RESERVED_06": null,
      "RESERVED_05": null,
      "RESERVED_04": null,
      "RESERVED_03": null,
      "RESERVED_02": null,
      "RESERVED_01": null,
      "RECORD_STATUS": null,
      "CURR_NO": "3",
      "INPUTTER": [
         {
            "seq": "1",
            "value": "58029_OFS.USER___OFS_METRO.FICO.OFS"
         }
      ],
      "DATE_TIME": [
         {
            "seq": "1",
            "value": "1602252215"
         }
      ],
      "AUTHORISER": "58029_OFS.USER_OFS_METRO.FICO.OFS",
      "CO_CODE": "GB0010008",
      "DEPT_CODE": "1",
      "AUDITOR_CODE": null,
      "AUDIT_DATE_TIME": null,
      "JOINT_HOLDER": null,
      "ISA_ATTORN_ADD": null,
      "STMT_MNTH": null,
      "STMT_BAL": null,
      "TOT_OS": null,
      "MIN_PYMT_DUE": null,
      "MIN_PYMT_DATE": null,
      "REASON_CODE": null,
      "REASON_TEXT": null,
      "ADD_ADDR_TYPE": null,
      "ADD_HOUSE_NO": null,
      "ADD_STRT": null,
      "ADD_TOWN": null,
      "ADD_POSTCODE": null,
      "ADD_CNTRY": null,
      "TIME_AT_ADDRESS": null,
      "RES_STATUS": null,
      "RES_VALUE": null,
      "MORTG_AMT": null,
      "EMP_ADDR_TYPE": null,
      "EMP_HOUSE_NO": null,
      "EMP_STREET": null,
      "EMP_TOWN": null,
      "EMP_POSTCODE": null,
      "EMP_COUNTRY": null,
      "EMP_TIME_AT_ADD": null,
      "EMP_RES_STATUS": null,
      "EMP_RES_VALUE": null,
      "EMP_MORTG_AMT": null,
      "EMP_OCCUPATION": null,
      "DATE_FROM": null,
      "EMPLOYER_NAME": null,
      "EMP_BUSINESS": null,
      "MTR_PRD_ADV_LIM": null,
      "MINI_STMT": null,
      "ALERT_AMT": null,
      "SENDS_ALERTS": null,
      "MET_AD_CARD_HOL": null,
      "METR_PREV_ALERT": null,
      "REL_CUSTOMER": null,
      "REL_CODE": null,
      "AC_CUST_REMOVED": null,
      "AC_REMOVAL_REASON": null,
      "ACCOUNT_TITLE_1": [
         {
            "seq": "",
            "value": "AT1-10929927"
         }
      ],
      "ACCOUNT_TITLE_2": null,
      "SHORT_TITLE": [
         {
            "seq":  "",
            "value": "SH-10929927"
         }
      ],
      "POSTING_RESTRICT": null,
      "ACCT_CREDIT_INT": null,
      "ACCT_DEBIT_INT": null,
      "RELATION_CODE": null,
      "JOINT_NOTES": null,
      "PENDING_ID": null,
      "TOTAL_PENDING": null,
      "FROM_DATE": null,
      "LOCKED_AMOUNT": null,
      "AVAILABLE_DATE": null,
      "AV_AUTH_DB_MVMT": null,
      "AVAILABLE_BAL": null,
      "OVERRIDE": null
}
""".trimIndent()

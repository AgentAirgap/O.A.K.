package com.agent.airgap.tools

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import org.json.JSONObject

object ToolRegistry {
    private const val TAG = "ToolRegistry"

    fun dispatch(context: Context, jsonOutput: String): Boolean {
        return try {
            val root = JSONObject(jsonOutput)
            val toolName = root.getString("name")
            val args = root.optJSONObject("arguments") ?: JSONObject()

            when (toolName) {
                "confirm_connection", "save_and_confirm_contact" -> {
                    val name = args.getString("name")
                    val phone = args.getString("phone")
                    saveContact(context, name, phone)
                    sendSms(phone, "Connection confirmation")
                    true
                }
                "send_sms" -> {
                    val phone = args.getString("phone")
                    val message = args.getString("message")
                    sendSms(phone, message)
                    true
                }
                else -> {
                    Log.w(TAG, "Unknown tool: $toolName")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing tool", e)
            false
        }
    }

    private fun saveContact(context: Context, name: String, phone: String) {
        val ops = ArrayList<ContentProviderOperation>()

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        )

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun sendSms(phone: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phone, null, message, null, null)
    }
}
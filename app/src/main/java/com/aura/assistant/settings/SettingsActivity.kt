package com.aura.assistant.settings

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.assistant.R
import com.aura.assistant.data.db.entities.TrustLevel
import com.aura.assistant.databinding.ActivitySettingsBinding

/**
 * Settings screen that lets users manage trusted contacts and their trust levels.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var adapter: TrustedContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupRecyclerView() {
        adapter = TrustedContactsAdapter(
            onEditClick = { contact -> showEditContactDialog(contact) },
            onDeleteClick = { contact ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_contact_title)
                    .setMessage(getString(R.string.delete_contact_message, contact.name))
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteContact(contact) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            },
            onTrustLevelChange = { contact, level ->
                viewModel.updateTrustLevel(contact, level)
            }
        )
        binding.rvTrustedContacts.layoutManager = LinearLayoutManager(this)
        binding.rvTrustedContacts.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.contacts.observe(this) { contacts ->
            adapter.submitList(contacts)
            binding.tvEmptyState.visibility =
                if (contacts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun setupFab() {
        binding.fabAddContact.setOnClickListener { showAddContactDialog() }
    }

    private fun showAddContactDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, padding, padding, padding)
        }
        val nameInput = EditText(this).apply {
            hint = getString(R.string.hint_contact_name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val phoneInput = EditText(this).apply {
            hint = getString(R.string.hint_phone_number)
            inputType = InputType.TYPE_CLASS_PHONE
        }
        layout.addView(nameInput)
        layout.addView(phoneInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_contact_title)
            .setView(layout)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isBlank() || phone.isBlank()) {
                    Toast.makeText(this, R.string.error_fields_required, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addContact(name, phone, TrustLevel.MEDIUM)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditContactDialog(contact: com.aura.assistant.data.db.entities.TrustedContact) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, padding, padding, padding)
        }
        val nameInput = EditText(this).apply {
            hint = getString(R.string.hint_contact_name)
            setText(contact.name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val phoneInput = EditText(this).apply {
            hint = getString(R.string.hint_phone_number)
            setText(contact.phoneNumber)
            inputType = InputType.TYPE_CLASS_PHONE
        }
        layout.addView(nameInput)
        layout.addView(phoneInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_contact_title)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isBlank() || phone.isBlank()) {
                    Toast.makeText(this, R.string.error_fields_required, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateContact(contact.copy(name = name, phoneNumber = phone))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

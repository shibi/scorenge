# Project-specific R8 rules. Libraries in use (Room, Hilt, kotlinx.serialization,
# Navigation) ship their own consumer rules.

# Credential Manager finds its Play services provider by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

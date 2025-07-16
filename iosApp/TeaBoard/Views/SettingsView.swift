//
//  SettingsView.swift
//  TeaBoard
//
//  Vista de configuración de sincronización con Google Drive
//

import SwiftUI
import shared

struct SettingsView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) var dismiss

    @State private var syncEnabled: Bool = false
    @State private var isLoggedIn: Bool = false
    @State private var userEmail: String = ""
    @State private var userName: String = ""
    @State private var showingLoginAlert = false

    var body: some View {
        NavigationView {
            Form {
                // Sección de Sincronización
                Section(header: Text("Sincronización")) {
                    Toggle("Sincronizar con Google Drive", isOn: $syncEnabled)
                        .onChange(of: syncEnabled) { newValue in
                            handleSyncToggle(enabled: newValue)
                        }

                    if syncEnabled {
                        VStack(alignment: .leading, spacing: 8) {
                            Label(
                                "Las configuraciones se guardarán automáticamente en Google Drive",
                                systemImage: "info.circle"
                            )
                            .font(.caption)
                            .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 4)
                    }
                }

                // Sección de Cuenta Google
                if syncEnabled {
                    Section(header: Text("Cuenta de Google")) {
                        if isLoggedIn {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Image(systemName: "person.circle.fill")
                                        .font(.title)
                                        .foregroundColor(.blue)

                                    VStack(alignment: .leading) {
                                        Text(userName.isEmpty ? "Usuario" : userName)
                                            .font(.headline)
                                        Text(userEmail)
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                }

                                Button(role: .destructive, action: signOut) {
                                    Label("Cerrar sesión", systemImage: "rectangle.portrait.and.arrow.right")
                                }
                                .buttonStyle(.bordered)
                            }
                            .padding(.vertical, 4)
                        } else {
                            Button(action: signIn) {
                                Label("Iniciar sesión con Google", systemImage: "g.circle.fill")
                                    .font(.headline)
                            }
                            .buttonStyle(.borderedProminent)

                            Text("Nota: La integración con Google Drive requiere configuración adicional en Google Cloud Console.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .padding(.top, 4)
                        }
                    }
                }

                // Sección de Información
                Section(header: Text("Información")) {
                    HStack {
                        Text("Versión")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Plataforma")
                        Spacer()
                        Text("iOS")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Framework")
                        Spacer()
                        Text("Kotlin Multiplatform")
                            .foregroundColor(.secondary)
                    }
                }

                // Sección de Almacenamiento
                Section(header: Text("Almacenamiento Local")) {
                    HStack {
                        Text("Configuraciones guardadas")
                        Spacer()
                        Text("\(appState.buttonConfigs.count)")
                            .foregroundColor(.secondary)
                    }

                    Button(role: .destructive, action: clearLocalData) {
                        Label("Borrar datos locales", systemImage: "trash")
                    }
                }

                // Sección Acerca de
                Section(header: Text("Acerca de TeaBoard")) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("TeaBoard es una aplicación AAC (Comunicación Aumentativa y Alternativa) diseñada para personas con autismo.")
                            .font(.body)

                        Text("Características:")
                            .font(.headline)
                            .padding(.top, 8)

                        VStack(alignment: .leading, spacing: 4) {
                            Label("6 botones personalizables", systemImage: "square.grid.3x2")
                            Label("Imágenes y audio personalizado", systemImage: "photo.on.rectangle")
                            Label("Sincronización con Google Drive (opcional)", systemImage: "icloud")
                            Label("Modo offline completo", systemImage: "wifi.slash")
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Configuración")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cerrar") {
                        dismiss()
                    }
                }
            }
            .onAppear(perform: loadSettings)
            .alert("Google Sign-In", isPresented: $showingLoginAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("La integración con Google Drive requiere configurar Google Sign-In SDK para iOS. Por ahora, esta función está en desarrollo.")
            }
        }
    }

    // MARK: - Functions

    private func loadSettings() {
        syncEnabled = appState.preferencesProvider.getBoolean(key: "sync_enabled", defaultValue: false)
        isLoggedIn = appState.preferencesProvider.getBoolean(key: "is_logged_in", defaultValue: false)
        userEmail = appState.preferencesProvider.getString(key: "user_email", defaultValue: "")
        userName = appState.preferencesProvider.getString(key: "user_name", defaultValue: "")
    }

    private func handleSyncToggle(enabled: Bool) {
        if enabled && !isLoggedIn {
            // Si se habilita sync pero no hay sesión, pedir login
            signIn()
        } else {
            // Guardar preferencia
            appState.preferencesProvider.putBoolean(key: "sync_enabled", value: enabled)
            appState.syncEnabled = enabled
        }
    }

    private func signIn() {
        // TODO: Implementar Google Sign-In para iOS
        // Requiere agregar GoogleSignIn SDK al proyecto
        showingLoginAlert = true

        // Por ahora, simulamos login para desarrollo
        #if DEBUG
        isLoggedIn = true
        userEmail = "usuario@example.com"
        userName = "Usuario de Prueba"
        appState.preferencesProvider.putBoolean(key: "is_logged_in", value: true)
        appState.preferencesProvider.putString(key: "user_email", value: userEmail)
        appState.preferencesProvider.putString(key: "user_name", value: userName)
        #endif
    }

    private func signOut() {
        // Limpiar preferencias
        appState.preferencesProvider.putBoolean(key: "is_logged_in", value: false)
        appState.preferencesProvider.putBoolean(key: "sync_enabled", value: false)
        appState.preferencesProvider.putString(key: "user_email", value: "")
        appState.preferencesProvider.putString(key: "user_name", value: "")

        // Actualizar estado
        isLoggedIn = false
        syncEnabled = false
        userEmail = ""
        userName = ""
        appState.syncEnabled = false
    }

    private func clearLocalData() {
        // Confirmar con el usuario
        let alert = UIAlertController(
            title: "¿Borrar datos locales?",
            message: "Esto eliminará todas las configuraciones de botones guardadas localmente. Esta acción no se puede deshacer.",
            preferredStyle: .alert
        )

        alert.addAction(UIAlertAction(title: "Cancelar", style: .cancel))
        alert.addAction(UIAlertAction(title: "Borrar", style: .destructive) { _ in
            // Borrar todas las configuraciones
            for buttonId in 1...6 {
                appState.deleteButtonConfig(buttonId: Int32(buttonId))
            }
        })

        // Presentar alert
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootViewController = windowScene.windows.first?.rootViewController {
            rootViewController.present(alert, animated: true)
        }
    }
}

#Preview {
    SettingsView()
        .environmentObject(AppState())
}

//
//  TeaBoardApp.swift
//  TeaBoard
//
//  Created by Claude Code
//  Copyright © 2025 TeaBoard. All rights reserved.
//

import SwiftUI
import shared

@main
struct TeaBoardApp: App {
    // Inicializar servicios compartidos
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            MainView()
                .environmentObject(appState)
        }
    }
}

/// Estado global de la aplicación
class AppState: ObservableObject {
    let fileProvider: FileProvider
    let preferencesProvider: PreferencesProvider
    let localStorage: IOSLocalStorageImpl
    let driveStorage: IOSDriveStorageImpl
    let storageService: StorageService
    let audioPlayer: IOSAudioPlayer
    let audioRecorder: IOSAudioRecorder

    @Published var buttonConfigs: [ButtonConfig] = []
    @Published var isEditMode: Bool = false
    @Published var syncEnabled: Bool = false

    init() {
        // Inicializar providers
        self.fileProvider = FileProvider()
        self.preferencesProvider = PreferencesProvider()

        // Inicializar storage
        self.localStorage = IOSLocalStorageImpl(fileProvider: fileProvider)
        self.driveStorage = IOSDriveStorageImpl(preferences: preferencesProvider)

        // Inicializar storage service
        self.storageService = StorageService(
            localStorage: localStorage,
            driveStorage: driveStorage,
            preferences: preferencesProvider
        )

        // Inicializar audio
        self.audioPlayer = IOSAudioPlayer()
        self.audioRecorder = IOSAudioRecorder()

        // Cargar configuraciones
        loadConfigs()
        loadSyncState()
    }

    func loadConfigs() {
        Task {
            do {
                let configs = try await storageService.getAllButtonConfigs()
                await MainActor.run {
                    self.buttonConfigs = configs
                }
            } catch {
                print("Error loading configs: \(error)")
            }
        }
    }

    func loadSyncState() {
        syncEnabled = preferencesProvider.getBoolean(key: "sync_enabled", defaultValue: false)
    }

    func saveButtonConfig(_ config: ButtonConfig) {
        Task {
            do {
                try await storageService.saveButtonConfig(config: config)
                loadConfigs()
            } catch {
                print("Error saving config: \(error)")
            }
        }
    }

    func deleteButtonConfig(buttonId: Int32) {
        Task {
            do {
                try await storageService.deleteButtonConfig(buttonId: buttonId)
                loadConfigs()
            } catch {
                print("Error deleting config: \(error)")
            }
        }
    }

    func getConfig(for buttonId: Int32) -> ButtonConfig? {
        return buttonConfigs.first { $0.buttonId == buttonId }
    }
}

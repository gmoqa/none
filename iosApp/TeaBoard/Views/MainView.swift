//
//  MainView.swift
//  TeaBoard
//
//  Vista principal con grid de 6 botones configurables
//

import SwiftUI
import shared

struct MainView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedButtonId: Int32?
    @State private var showingConfigureSheet = false
    @State private var showingSettings = false

    // Grid layout: 3 columnas
    let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        NavigationView {
            ZStack {
                // Fondo con gradiente (similar a Android)
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(red: 0.95, green: 0.98, blue: 1.0),
                        Color(red: 0.85, green: 0.92, blue: 0.98)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Header con título y botones
                    HStack {
                        Text("TeaBoard")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(.blue)

                        Spacer()

                        // Botón de edición
                        Button(action: {
                            withAnimation {
                                appState.isEditMode.toggle()
                            }
                        }) {
                            Image(systemName: appState.isEditMode ? "checkmark.circle.fill" : "pencil.circle")
                                .font(.title2)
                                .foregroundColor(appState.isEditMode ? .green : .blue)
                        }

                        // Botón de settings
                        Button(action: {
                            showingSettings = true
                        }) {
                            Image(systemName: "gear")
                                .font(.title2)
                                .foregroundColor(.blue)
                        }
                    }
                    .padding()
                    .background(Color.white.opacity(0.9))

                    // Grid de botones
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(1...6, id: \.self) { buttonId in
                                ButtonCard(
                                    buttonId: Int32(buttonId),
                                    config: appState.getConfig(for: Int32(buttonId)),
                                    isEditMode: appState.isEditMode,
                                    onTap: {
                                        handleButtonTap(buttonId: Int32(buttonId))
                                    },
                                    onConfigure: {
                                        selectedButtonId = Int32(buttonId)
                                        showingConfigureSheet = true
                                    }
                                )
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showingConfigureSheet) {
                if let buttonId = selectedButtonId {
                    ConfigureButtonView(buttonId: buttonId)
                        .environmentObject(appState)
                }
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView()
                    .environmentObject(appState)
            }
        }
    }

    private func handleButtonTap(buttonId: Int32) {
        if appState.isEditMode {
            // Modo edición: abrir configuración
            selectedButtonId = buttonId
            showingConfigureSheet = true
        } else {
            // Modo uso: reproducir audio
            if let config = appState.getConfig(for: buttonId),
               !config.audioPath.isEmpty {
                appState.audioPlayer.playAudio(
                    audioPath: config.audioPath,
                    onComplete: nil,
                    onError: { error in
                        print("Error playing audio: \(error)")
                    }
                )
            }
        }
    }
}

/// Tarjeta individual de botón
struct ButtonCard: View {
    let buttonId: Int32
    let config: ButtonConfig?
    let isEditMode: Bool
    let onTap: () -> Void
    let onConfigure: () -> Void

    // Colores de botones (usando constantes compartidas de ButtonConstants.Colors.iOS)
    private var buttonColor: Color {
        switch buttonId {
        case 1: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_1_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_1_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_1_BLUE
        )
        case 2: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_2_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_2_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_2_BLUE
        )
        case 3: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_3_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_3_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_3_BLUE
        )
        case 4: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_4_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_4_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_4_BLUE
        )
        case 5: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_5_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_5_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_5_BLUE
        )
        case 6: return Color(
            red: ButtonConstants.Colors.iOS.shared.BUTTON_6_RED,
            green: ButtonConstants.Colors.iOS.shared.BUTTON_6_GREEN,
            blue: ButtonConstants.Colors.iOS.shared.BUTTON_6_BLUE
        )
        default: return Color.gray.opacity(0.3)
        }
    }

    var body: some View {
        Button(action: onTap) {
            ZStack {
                RoundedRectangle(cornerRadius: 16)
                    .fill(buttonColor)
                    .shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)

                VStack(spacing: 12) {
                    // Imagen o placeholder
                    if let config = config, !config.imagePath.isEmpty {
                        AsyncImage(url: URL(fileURLWithPath: config.imagePath)) { image in
                            image
                                .resizable()
                                .scaledToFill()
                        } placeholder: {
                            Image(systemName: "photo")
                                .font(.system(size: 40))
                                .foregroundColor(.gray.opacity(0.5))
                        }
                        .frame(width: 80, height: 80)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    } else {
                        Image(systemName: "photo")
                            .font(.system(size: 40))
                            .foregroundColor(.gray.opacity(0.5))
                    }

                    // Label
                    Text(config?.label ?? "Botón \(buttonId)")
                        .font(.headline)
                        .foregroundColor(.black)
                        .lineLimit(2)
                        .multilineTextAlignment(.center)

                    // Indicador de audio
                    if let config = config, !config.audioPath.isEmpty {
                        Image(systemName: "speaker.wave.2.fill")
                            .font(.caption)
                            .foregroundColor(.blue.opacity(0.7))
                    }
                }
                .padding()

                // Botón de edición (solo visible en modo edición)
                if isEditMode {
                    VStack {
                        HStack {
                            Spacer()
                            Button(action: onConfigure) {
                                Image(systemName: "pencil.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                                    .background(Color.white.clipShape(Circle()))
                            }
                            .padding(8)
                        }
                        Spacer()
                    }
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
        .frame(height: 180)
    }
}

#Preview {
    MainView()
        .environmentObject(AppState())
}

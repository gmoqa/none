//
//  ConfigureButtonView.swift
//  TeaBoard
//
//  Vista para configurar imagen, audio y label de un botón
//

import SwiftUI
import PhotosUI
import shared

struct ConfigureButtonView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.dismiss) var dismiss

    let buttonId: Int32

    @State private var label: String = ""
    @State private var imagePath: String = ""
    @State private var audioPath: String = ""
    @State private var selectedImage: UIImage?
    @State private var showingImagePicker = false
    @State private var showingCamera = false
    @State private var isRecording = false
    @State private var recordingTime: TimeInterval = 0
    @State private var recordingTimer: Timer?

    var body: some View {
        NavigationView {
            Form {
                // Sección de Label
                Section(header: Text("Etiqueta")) {
                    TextField("Nombre del botón", text: $label)
                        .font(.headline)
                }

                // Sección de Imagen
                Section(header: Text("Imagen")) {
                    if let image = selectedImage {
                        Image(uiImage: image)
                            .resizable()
                            .scaledToFit()
                            .frame(height: 200)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    } else if !imagePath.isEmpty {
                        AsyncImage(url: URL(fileURLWithPath: imagePath)) { image in
                            image
                                .resizable()
                                .scaledToFit()
                                .frame(height: 200)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        } placeholder: {
                            ProgressView()
                        }
                    } else {
                        Image(systemName: "photo")
                            .font(.system(size: 100))
                            .foregroundColor(.gray.opacity(0.5))
                            .frame(maxWidth: .infinity)
                            .frame(height: 200)
                    }

                    HStack(spacing: 16) {
                        Button(action: {
                            showingCamera = true
                        }) {
                            Label("Cámara", systemImage: "camera.fill")
                        }
                        .buttonStyle(.bordered)

                        Button(action: {
                            showingImagePicker = true
                        }) {
                            Label("Galería", systemImage: "photo.on.rectangle")
                        }
                        .buttonStyle(.bordered)

                        if !imagePath.isEmpty || selectedImage != nil {
                            Button(role: .destructive, action: {
                                selectedImage = nil
                                imagePath = ""
                            }) {
                                Label("Borrar", systemImage: "trash")
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }

                // Sección de Audio
                Section(header: Text("Audio")) {
                    if !audioPath.isEmpty {
                        HStack {
                            Button(action: playAudio) {
                                Label("Reproducir", systemImage: "play.circle.fill")
                                    .font(.headline)
                            }
                            .buttonStyle(.bordered)

                            Spacer()

                            Button(role: .destructive, action: {
                                audioPath = ""
                            }) {
                                Label("Borrar", systemImage: "trash")
                            }
                            .buttonStyle(.bordered)
                        }
                    }

                    // Botón de grabación
                    Button(action: toggleRecording) {
                        HStack {
                            Image(systemName: isRecording ? "stop.circle.fill" : "mic.circle.fill")
                                .font(.title2)
                                .foregroundColor(isRecording ? .red : .blue)

                            if isRecording {
                                Text("Detener grabación")
                                Spacer()
                                Text(formatTime(recordingTime))
                                    .foregroundColor(.red)
                                    .monospacedDigit()
                            } else {
                                Text("Grabar audio")
                            }
                        }
                    }
                    .buttonStyle(.bordered)
                    .tint(isRecording ? .red : .blue)
                }

                // Sección de Acciones
                Section {
                    Button(action: saveConfiguration) {
                        HStack {
                            Spacer()
                            Label("Guardar", systemImage: "checkmark.circle.fill")
                                .font(.headline)
                            Spacer()
                        }
                    }
                    .buttonStyle(.borderedProminent)

                    Button(role: .destructive, action: deleteConfiguration) {
                        HStack {
                            Spacer()
                            Label("Eliminar configuración", systemImage: "trash.fill")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Configurar Botón \(buttonId)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancelar") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingImagePicker) {
                ImagePicker(image: $selectedImage, sourceType: .photoLibrary)
            }
            .sheet(isPresented: $showingCamera) {
                ImagePicker(image: $selectedImage, sourceType: .camera)
            }
            .onAppear(perform: loadConfiguration)
        }
    }

    // MARK: - Functions

    private func loadConfiguration() {
        if let config = appState.getConfig(for: buttonId) {
            label = config.label
            imagePath = config.imagePath
            audioPath = config.audioPath
        } else {
            label = "Botón \(buttonId)"
        }
    }

    private func saveConfiguration() {
        Task {
            var finalImagePath = imagePath
            var finalAudioPath = audioPath

            // Guardar imagen si hay una nueva seleccionada
            if let image = selectedImage {
                if let imageData = image.jpegData(compressionQuality: 0.8) {
                    do {
                        let imageFile = appState.fileProvider.getFile(
                            subdirectory: "images",
                            filename: "button_\(buttonId)_\(Date().timeIntervalSince1970).jpg"
                        )
                        try await imageFile.writeBytes(bytes: KotlinByteArray(size: Int32(imageData.count)) { index in
                            return Int8(bitPattern: imageData[Int(index)])
                        })
                        finalImagePath = imageFile.path
                    } catch {
                        print("Error saving image: \(error)")
                    }
                }
            }

            // Crear configuración
            let config = ButtonConfig(
                buttonId: buttonId,
                label: label,
                imagePath: finalImagePath,
                audioPath: finalAudioPath,
                driveImageId: "",
                driveAudioId: ""
            )

            // Guardar
            appState.saveButtonConfig(config)

            // Cerrar
            await MainActor.run {
                dismiss()
            }
        }
    }

    private func deleteConfiguration() {
        appState.deleteButtonConfig(buttonId: buttonId)
        dismiss()
    }

    private func toggleRecording() {
        if isRecording {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private func startRecording() {
        if let recordedFile = appState.audioRecorder.startRecording(buttonId: buttonId) {
            isRecording = true
            recordingTime = 0

            // Iniciar timer para mostrar tiempo de grabación
            recordingTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
                recordingTime += 0.1
            }
        }
    }

    private func stopRecording() {
        recordingTimer?.invalidate()
        recordingTimer = nil

        if let recordedFile = appState.audioRecorder.stopRecording() {
            audioPath = recordedFile.path
            isRecording = false
        }
    }

    private func playAudio() {
        guard !audioPath.isEmpty else { return }
        appState.audioPlayer.playAudio(
            audioPath: audioPath,
            onComplete: nil,
            onError: { error in
                print("Error playing audio: \(error)")
            }
        )
    }

    private func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        let milliseconds = Int((time.truncatingRemainder(dividingBy: 1)) * 10)
        return String(format: "%02d:%02d.%01d", minutes, seconds, milliseconds)
    }
}

// MARK: - Image Picker

struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    var sourceType: UIImagePickerController.SourceType
    @Environment(\.dismiss) var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.image = image
            }
            parent.dismiss()
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

#Preview {
    ConfigureButtonView(buttonId: 1)
        .environmentObject(AppState())
}

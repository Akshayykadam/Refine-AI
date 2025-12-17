import { StatusBar } from 'expo-status-bar';
import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Linking, Platform, useColorScheme, ScrollView, SafeAreaView, NativeModules, NativeEventEmitter, ActivityIndicator, AppState } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const PRIMARY_COLOR = '#15C39A';
const { LocalAIModule } = NativeModules;

export default function App() {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === 'dark';
  const styles = getStyles(isDark);

  const [isModelDownloaded, setIsModelDownloaded] = useState<boolean | null>(null);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [downloadStatus, setDownloadStatus] = useState('');
  const [isServiceEnabled, setIsServiceEnabled] = useState(false);

  const checkServiceStatus = async () => {
    if (LocalAIModule && LocalAIModule.isAccessibilityServiceEnabled) {
      const enabled = await LocalAIModule.isAccessibilityServiceEnabled();
      setIsServiceEnabled(enabled);
    }
  };

  useEffect(() => {
    // Check initial status
    checkServiceStatus();

    // Check on app resume
    const subscription = AppState.addEventListener('change', nextAppState => {
      if (nextAppState === 'active') {
        checkServiceStatus();
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  useEffect(() => {
    // Check if model is downloaded on mount
    if (LocalAIModule) {
      LocalAIModule.isModelDownloaded().then((downloaded: boolean) => {
        setIsModelDownloaded(downloaded);
      });

      // Setup event listeners
      const eventEmitter = new NativeEventEmitter(LocalAIModule);

      const progressListener = eventEmitter.addListener('onDownloadProgress', (event) => {
        setDownloadProgress(event.percent);
        const mbDownloaded = Math.round(event.bytesDownloaded / (1024 * 1024));
        const mbTotal = Math.round(event.totalBytes / (1024 * 1024));
        setDownloadStatus(`${mbDownloaded} / ${mbTotal} MB`);
      });

      const completeListener = eventEmitter.addListener('onDownloadComplete', () => {
        setIsDownloading(false);
        setIsModelDownloaded(true);
        setDownloadProgress(0);
        setDownloadStatus('');
      });

      const errorListener = eventEmitter.addListener('onDownloadError', (event) => {
        setIsDownloading(false);
        setDownloadStatus(`Error: ${event.error}`);
      });

      return () => {
        progressListener.remove();
        completeListener.remove();
        errorListener.remove();
      };
    }
  }, []);

  const openSettings = () => {
    if (Platform.OS === 'android') {
      Linking.sendIntent('android.settings.ACCESSIBILITY_SETTINGS');
    }
  };

  const openGitHub = () => {
    Linking.openURL('https://github.com/Akshayykadam');
  };

  const startDownload = () => {
    if (LocalAIModule) {
      setIsDownloading(true);
      setDownloadProgress(0);
      setDownloadStatus('Starting download...');
      LocalAIModule.downloadModel();
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar style={isDark ? "light" : "dark"} />
      <ScrollView contentContainerStyle={styles.scrollContent}>

        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.appTitle}>Refine.AI</Text>
          <Text style={styles.tagline}>Elevate your writing, everywhere.</Text>
        </View>

        {/* Main Action Card */}
        {/* Main Action Card */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Get Started</Text>

          {isServiceEnabled ? (
            <View>
              <Text style={styles.cardDescription}>
                Service is active! You can now use Refine.AI in other apps.
              </Text>
              <View style={[styles.stepsContainer, { alignItems: 'center', marginVertical: 20 }]}>
                <Ionicons name="checkmark-circle" size={64} color={PRIMARY_COLOR} />
                <Text style={[styles.stepText, { fontWeight: 'bold', marginTop: 10 }]}>You're all set!</Text>
              </View>
              <TouchableOpacity style={[styles.secondaryButton]} onPress={openSettings}>
                <Text style={styles.secondaryButtonText}>Open Settings</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View>
              <Text style={styles.cardDescription}>
                Enable the Accessibility Service to let Refine.AI assist you in any app.
              </Text>

              <View style={styles.stepsContainer}>
                <View style={styles.stepRow}>
                  <Text style={styles.stepNumber}>1</Text>
                  <Text style={styles.stepText}>Tap the button below</Text>
                </View>
                <View style={styles.stepRow}>
                  <Text style={styles.stepNumber}>2</Text>
                  <Text style={styles.stepText}>Find "Refine.AI" in the list</Text>
                </View>
                <View style={styles.stepRow}>
                  <Text style={styles.stepNumber}>3</Text>
                  <Text style={styles.stepText}>Toggle it ON</Text>
                </View>
              </View>

              <TouchableOpacity style={styles.primaryButton} onPress={openSettings}>
                <Text style={styles.primaryButtonText}>Enable Service</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>

        {/* Local AI Section - Only show if model not downloaded */}
        {isModelDownloaded === false && (
          <View style={styles.card}>
            <Text style={styles.cardDescription}>
              Download the AI model to enable offline text processing. This requires ~1.4 GB of storage.
            </Text>

            {isDownloading ? (
              <View style={styles.progressContainer}>
                <View style={styles.progressBar}>
                  <View style={[styles.progressFill, { width: `${downloadProgress}%` }]} />
                </View>
                <Text style={styles.progressText}>{downloadProgress}% - {downloadStatus}</Text>
              </View>
            ) : (
              <TouchableOpacity style={[styles.secondaryButton, { marginTop: 16 }]} onPress={startDownload}>
                <Text style={styles.secondaryButtonText}>Download Model</Text>
              </TouchableOpacity>
            )}
          </View>
        )}

        {/* About Section */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>About</Text>
          <Text style={styles.cardDescription}>
            Refine.AI is a system-wide writing assistant powered by Gemini AI with optional on-device processing via Gemma 1B. Rewrite, correct, and tone-switch your text in any app.
          </Text>
          <Text style={[styles.cardDescription, { marginTop: 10 }]}>
            Privacy First: With Local AI mode, your text never leaves your device. Cloud mode uses encrypted transmission only when you tap Rewrite.
          </Text>
        </View>

        {/* Developer Section */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Developer</Text>
          <Text style={styles.cardDescription}>Akshay Kadam</Text>
          <TouchableOpacity style={[styles.primaryButton, { marginTop: 16 }]} onPress={openGitHub}>
            <Text style={styles.primaryButtonText}>View on GitHub</Text>
          </TouchableOpacity>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const getStyles = (isDark: boolean) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: isDark ? '#121212' : '#F5F5F7',
  },
  scrollContent: {
    padding: 24,
    paddingTop: 60,
  },
  header: {
    marginBottom: 40,
    alignItems: 'center',
  },
  appTitle: {
    fontSize: 32,
    fontWeight: '800',
    color: isDark ? '#FFFFFF' : '#1C1C1E',
    marginBottom: 8,
    letterSpacing: 0.5,
  },
  tagline: {
    fontSize: 16,
    color: isDark ? '#A1A1A6' : '#86868B',
    fontWeight: '500',
  },
  card: {
    backgroundColor: isDark ? '#1C1C1E' : '#FFFFFF',
    borderRadius: 20,
    padding: 24,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: isDark ? 0.3 : 0.05,
    shadowRadius: 12,
    elevation: 4,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: isDark ? '#FFFFFF' : '#1C1C1E',
    marginBottom: 12,
  },
  cardDescription: {
    fontSize: 15,
    color: isDark ? '#D1D1D6' : '#636366',
    lineHeight: 22,
  },
  stepsContainer: {
    marginTop: 16,
    marginBottom: 24,
  },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  stepNumber: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: isDark ? '#2C2C2E' : '#E5E5EA',
    color: PRIMARY_COLOR,
    textAlign: 'center',
    textAlignVertical: 'center', // Android
    lineHeight: 24, // iOS
    fontSize: 14,
    fontWeight: 'bold',
    marginRight: 12,
    overflow: 'hidden',
  },
  stepText: {
    fontSize: 15,
    color: isDark ? '#E5E5EA' : '#3A3A3C',
  },
  primaryButton: {
    backgroundColor: PRIMARY_COLOR,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    shadowColor: PRIMARY_COLOR,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  primaryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    marginTop: 16,
    backgroundColor: isDark ? '#2C2C2E' : '#F2F2F7',
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
  },
  secondaryButtonText: {
    color: isDark ? '#FFFFFF' : '#1C1C1E',
    fontSize: 16,
    fontWeight: '600',
  },
  progressContainer: {
    marginTop: 16,
  },
  progressBar: {
    height: 8,
    backgroundColor: isDark ? '#2C2C2E' : '#E5E5EA',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: PRIMARY_COLOR,
    borderRadius: 4,
  },
  progressText: {
    marginTop: 8,
    fontSize: 13,
    color: isDark ? '#A1A1A6' : '#86868B',
    textAlign: 'center',
  },
});

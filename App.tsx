import { StatusBar } from 'expo-status-bar';
import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet, Text, View, TouchableOpacity, Linking, Platform,
  ScrollView, SafeAreaView, NativeModules, AppState, Dimensions,
  FlatList, useColorScheme, TextInput, ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';

const { width } = Dimensions.get('window');

// Monochrome Color Palettes
const COLORS = {
  dark: {
    primary: '#FFFFFF',
    primaryDark: '#E5E5E5',
    accent: '#999999',
    background: '#000000',
    cardBg: '#1A1A1A',
    cardBorder: '#333333',
    textPrimary: '#FFFFFF',
    textSecondary: '#999999',
    stepBg: '#2A2A2A',
    success: '#FFFFFF',
    buttonText: '#000000',
  },
  light: {
    primary: '#1A1A1A',
    primaryDark: '#000000',
    accent: '#666666',
    background: '#FFFFFF',
    cardBg: '#FFFFFF',
    cardBorder: '#E5E5E5',
    textPrimary: '#1A1A1A',
    textSecondary: '#666666',
    stepBg: '#F5F5F5',
    success: '#1A1A1A',
    buttonText: '#FFFFFF',
  }
};

const { GeminiModule } = NativeModules;

// Onboarding slides data
const onboardingSlides = [
  {
    id: '1',
    icon: 'sparkles',
    title: 'AI-Powered Writing',
    description: 'Rewrite, refine, and transform your text with the power of Gemma 3 AI.',
  },
  {
    id: '2',
    icon: 'apps',
    title: 'Works Everywhere',
    description: 'Select any text in any app - WhatsApp, Email, Notes, or anywhere else.',
  },
  {
    id: '3',
    icon: 'flash',
    title: 'One Tap Magic',
    description: 'Just select text, tap the floating bubble, choose a style, and you\'re done!',
  },
];

export default function App() {
  const colorScheme = useColorScheme();
  const isDark = colorScheme === 'dark';
  const colors = isDark ? COLORS.dark : COLORS.light;
  const styles = getStyles(colors, isDark);

  const [showOnboarding, setShowOnboarding] = useState(true);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isServiceEnabled, setIsServiceEnabled] = useState(false);
  const flatListRef = useRef<FlatList>(null);

  const [inputText, setInputText] = useState('');
  const [outputText, setOutputText] = useState('');
  const [selectedStyle, setSelectedStyle] = useState('Refine');
  const [isLoading, setIsLoading] = useState(false);

  const stylesList = ['Refine', 'Grammar', 'Professional', 'Casual', 'Concise', 'Warm', 'Love', 'Emojify', 'Hinglish'];

  const getInstruction = (style: string) => {
    switch (style) {
      case 'Warm': return "Rewrite the text in a warm, supportive, and human tone. Sound approachable, respectful, and emotionally aware without being overly sentimental.";
      case 'Love': return "Rewrite the text with gentle affection and care, expressing warmth, appreciation, and emotional closeness.";
      case 'Emojify': return "Rewrite the text to be clear and engaging, then add a small number of relevant emojis to enhance expression.";
      case 'Hinglish': return "Rewrite the text in casual Hinglish (a natural mix of Hindi and English) as spoken by urban Indians. Use common Hindi words written in Roman script.";
      case 'Refine': return "Rewrite the text to be clearer, more fluent, and easier to read while preserving the original meaning.";
      case 'Grammar': return "Check the text for grammar, spelling, punctuation, and basic sentence structure errors. Correct only what is necessary.";
      case 'Professional': return "Rewrite the text in a professional, polished, and confident tone. Use clear, concise language suitable for workplace.";
      case 'Casual': return "Rewrite the text in a relaxed, friendly, and conversational tone. Keep it simple and natural.";
      case 'Concise': return "Rewrite the text to be shorter and more concise without losing key information.";
      default: return "Refine this text";
    }
  };

  const handleRewrite = async () => {
    if (!inputText.trim()) return;
    setIsLoading(true);
    setOutputText('');
    try {
      const instruction = getInstruction(selectedStyle);
      const result = await GeminiModule.generateContent(inputText, instruction);
      setOutputText(result);
    } catch (error) {
      console.error(error);
      setOutputText('Failed to generate. Please check your internet or API key.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    AsyncStorage.getItem('onboarding_complete').then((value) => {
      if (value === 'true') {
        setShowOnboarding(false);
      }
    });
  }, []);

  const checkServiceStatus = async () => {
    if (GeminiModule && GeminiModule.isAccessibilityServiceEnabled) {
      const enabled = await GeminiModule.isAccessibilityServiceEnabled();
      setIsServiceEnabled(enabled);
    }
  };

  useEffect(() => {
    checkServiceStatus();
    const subscription = AppState.addEventListener('change', nextAppState => {
      if (nextAppState === 'active') {
        checkServiceStatus();
      }
    });
    return () => subscription.remove();
  }, []);

  const completeOnboarding = async () => {
    await AsyncStorage.setItem('onboarding_complete', 'true');
    setShowOnboarding(false);
  };

  const openSettings = () => {
    if (Platform.OS === 'android') {
      Linking.sendIntent('android.settings.ACCESSIBILITY_SETTINGS');
    }
  };

  const openGitHub = () => {
    Linking.openURL('https://github.com/Akshayykadam');
  };

  const renderOnboardingSlide = ({ item }: { item: typeof onboardingSlides[0] }) => (
    <View style={styles.slide}>
      <View style={styles.slideIconContainer}>
        <Ionicons name={item.icon as any} size={48} color={colors.buttonText} />
      </View>
      <Text style={styles.slideTitle}>{item.title}</Text>
      <Text style={styles.slideDescription}>{item.description}</Text>
    </View>
  );

  // Onboarding Screen
  if (showOnboarding) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar style={isDark ? "light" : "dark"} />
        <View style={styles.onboardingContainer}>
          <FlatList
            ref={flatListRef}
            data={onboardingSlides}
            renderItem={renderOnboardingSlide}
            horizontal
            pagingEnabled
            showsHorizontalScrollIndicator={false}
            onMomentumScrollEnd={(e) => {
              const index = Math.round(e.nativeEvent.contentOffset.x / width);
              setCurrentSlide(index);
            }}
            keyExtractor={(item) => item.id}
          />

          <View style={styles.pagination}>
            {onboardingSlides.map((_, index) => (
              <View
                key={index}
                style={[
                  styles.paginationDot,
                  currentSlide === index && styles.paginationDotActive,
                ]}
              />
            ))}
          </View>

          <View style={styles.onboardingButtons}>
            {currentSlide < onboardingSlides.length - 1 ? (
              <>
                <TouchableOpacity style={styles.skipButton} onPress={completeOnboarding}>
                  <Text style={styles.skipButtonText}>Skip</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.primaryButtonWrapper}
                  onPress={() => {
                    flatListRef.current?.scrollToIndex({ index: currentSlide + 1 });
                    setCurrentSlide(currentSlide + 1);
                  }}
                >
                  <View style={styles.primaryButton}>
                    <Text style={styles.primaryButtonText}>Next</Text>
                    <Ionicons name="arrow-forward" size={20} color={colors.buttonText} />
                  </View>
                </TouchableOpacity>
              </>
            ) : (
              <TouchableOpacity style={styles.getStartedWrapper} onPress={completeOnboarding}>
                <View style={styles.getStartedButton}>
                  <Text style={styles.getStartedText}>Get Started</Text>
                </View>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </SafeAreaView>
    );
  }

  // Main App Screen
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar style={isDark ? "light" : "dark"} />
      <ScrollView contentContainerStyle={styles.scrollContent}>

        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.appTitle}>Refine.AI</Text>
          <Text style={styles.tagline}>Elevate your writing, everywhere.</Text>
        </View>

        {/* Status Card */}
        {/* Status Card */}
        {isServiceEnabled ? (
          <View style={{
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 20,
            backgroundColor: colors.stepBg,
            paddingVertical: 8,
            paddingHorizontal: 16,
            borderRadius: 20,
            alignSelf: 'center',
            borderWidth: 1,
            borderColor: colors.cardBorder
          }}>
            <View style={{ width: 8, height: 8, borderRadius: 4, backgroundColor: '#4CAF50', marginRight: 8 }} />
            <Text style={{ color: colors.textPrimary, fontWeight: '600', fontSize: 13 }}>Ready to Use • Service Active</Text>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Enable Service</Text>
            <View>
              <Text style={styles.cardDescription}>
                Enable the Accessibility Service to let Refine.AI assist you.
              </Text>
              <View style={styles.stepsContainer}>
                {['Tap Enable Service below', 'Find "Refine.AI" in the list', 'Toggle it ON'].map((step, i) => (
                  <View key={i} style={styles.stepRow}>
                    <View style={styles.stepNumber}>
                      <Text style={styles.stepNumberText}>{i + 1}</Text>
                    </View>
                    <Text style={styles.stepText}>{step}</Text>
                  </View>
                ))}
              </View>
              <TouchableOpacity style={styles.primaryButtonWrapper} onPress={openSettings}>
                <View style={styles.primaryButton}>
                  <Text style={styles.primaryButtonText}>Enable Service</Text>
                </View>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* Try it Now */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Try it Now</Text>
          <TextInput
            style={[styles.input, { color: colors.textPrimary, borderColor: colors.cardBorder, backgroundColor: colors.stepBg }]}
            placeholder="Type or paste text here..."
            placeholderTextColor={colors.textSecondary}
            multiline
            numberOfLines={4}
            textAlignVertical="top"
            value={inputText}
            onChangeText={setInputText}
          />

          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipsContainer}>
            {stylesList.map((style) => (
              <TouchableOpacity
                key={style}
                style={[
                  styles.chip,
                  selectedStyle === style ? { backgroundColor: colors.primary } : { backgroundColor: colors.stepBg, borderWidth: 1, borderColor: colors.cardBorder }
                ]}
                onPress={() => setSelectedStyle(style)}
              >
                <Text style={[
                  styles.chipText,
                  selectedStyle === style ? { color: colors.buttonText } : { color: colors.textPrimary }
                ]}>{style}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>

          <TouchableOpacity
            style={[styles.primaryButtonWrapper, { marginTop: 16, opacity: inputText.trim() ? 1 : 0.6 }]}
            onPress={handleRewrite}
            disabled={!inputText.trim() || isLoading}
          >
            <View style={styles.primaryButton}>
              {isLoading ? (
                <ActivityIndicator color={colors.buttonText} />
              ) : (
                <>
                  <Ionicons name="sparkles" size={18} color={colors.buttonText} style={{ marginRight: 8 }} />
                  <Text style={styles.primaryButtonText}>Rewrite</Text>
                </>
              )}
            </View>
          </TouchableOpacity>

          {outputText ? (
            <View style={[styles.resultContainer, { backgroundColor: colors.stepBg, borderColor: colors.cardBorder }]}>
              <Text style={[styles.resultText, { color: colors.textPrimary }]}>{outputText}</Text>
              <View style={{ position: 'absolute', bottom: 12, right: 12 }}>
                <TouchableOpacity
                  onPress={() => {
                    GeminiModule.copyToClipboard(outputText);
                  }}
                  style={{ padding: 8 }}
                >
                  <Ionicons name="copy-outline" size={20} color={colors.textSecondary} />
                </TouchableOpacity>
              </View>
            </View>
          ) : null}
        </View>

        {/* How to Use */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>How to Use</Text>
          <View style={styles.howToStep}>
            <View style={styles.howToIcon}>
              <Ionicons name="text" size={24} color={colors.textPrimary} />
            </View>
            <View style={styles.howToContent}>
              <Text style={styles.howToTitle}>1. Select Text</Text>
              <Text style={styles.howToDesc}>Long press on any text in any app to select it</Text>
            </View>
          </View>
          <View style={styles.howToStep}>
            <View style={styles.howToIcon}>
              <Ionicons name="ellipse" size={24} color={colors.textPrimary} />
            </View>
            <View style={styles.howToContent}>
              <Text style={styles.howToTitle}>2. Tap the Bubble</Text>
              <Text style={styles.howToDesc}>A bubble will appear - tap it to open Refine.AI</Text>
            </View>
          </View>
          <View style={styles.howToStep}>
            <View style={styles.howToIcon}>
              <Ionicons name="options" size={24} color={colors.textPrimary} />
            </View>
            <View style={styles.howToContent}>
              <Text style={styles.howToTitle}>3. Choose a Style</Text>
              <Text style={styles.howToDesc}>Pick Professional, Casual, Concise, or other styles</Text>
            </View>
          </View>
          <View style={styles.howToStep}>
            <View style={styles.howToIcon}>
              <Ionicons name="checkmark-circle" size={24} color={colors.textPrimary} />
            </View>
            <View style={styles.howToContent}>
              <Text style={styles.howToTitle}>4. Insert & Done</Text>
              <Text style={styles.howToDesc}>Tap "Insert" to replace with the improved version</Text>
            </View>
          </View>
        </View>

        {/* About */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>About</Text>
          <View style={styles.featureRow}>
            <Ionicons name="sparkles" size={20} color={colors.textSecondary} />
            <Text style={styles.featureText}>Powered by Gemma 3 AI</Text>
          </View>
          <View style={styles.featureRow}>
            <Ionicons name="shield-checkmark" size={20} color={colors.textSecondary} />
            <Text style={styles.featureText}>Encrypted cloud transmission</Text>
          </View>
          <View style={styles.featureRow}>
            <Ionicons name="flash" size={20} color={colors.textSecondary} />
            <Text style={styles.featureText}>Works in any text field</Text>
          </View>
        </View>

        {/* Developer */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Developer</Text>
          <Text style={styles.cardDescription}>Akshay Kadam</Text>
          <TouchableOpacity style={[styles.primaryButtonWrapper, { marginTop: 12 }]} onPress={openGitHub}>
            <View style={styles.primaryButton}>
              <Ionicons name="logo-github" size={20} color={colors.buttonText} style={{ marginRight: 8 }} />
              <Text style={styles.primaryButtonText}>View on GitHub</Text>
            </View>
          </TouchableOpacity>
        </View>

        <Text style={styles.versionText}>Version 1.0.0</Text>

      </ScrollView>
    </SafeAreaView>
  );
}

const getStyles = (colors: typeof COLORS.dark, isDark: boolean) => StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    padding: 20,
    paddingTop: 50,
    paddingBottom: 40,
  },
  header: {
    marginBottom: 28,
    alignItems: 'center',
  },
  appTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: colors.textPrimary,
    marginBottom: 6,
    letterSpacing: 0.5,
  },
  tagline: {
    fontSize: 16,
    color: colors.textSecondary,
    fontWeight: '500',
  },
  card: {
    backgroundColor: colors.cardBg,
    borderRadius: 20,
    padding: 24,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: colors.cardBorder,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
    marginBottom: 12,
  },
  cardDescription: {
    fontSize: 15,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  stepsContainer: {
    marginTop: 16,
    marginBottom: 20,
  },
  stepRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 14,
  },
  stepNumber: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  stepNumberText: {
    color: colors.buttonText,
    fontSize: 14,
    fontWeight: 'bold',
  },
  stepText: {
    fontSize: 15,
    color: colors.textPrimary,
    flex: 1,
  },
  primaryButtonWrapper: {
    borderRadius: 14,
    overflow: 'hidden',
  },
  primaryButton: {
    paddingVertical: 16,
    paddingHorizontal: 24,
    borderRadius: 14,
    backgroundColor: colors.primary,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
  },
  primaryButtonText: {
    color: colors.buttonText,
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    backgroundColor: colors.stepBg,
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.cardBorder,
  },
  secondaryButtonText: {
    color: colors.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
  successIcon: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
  },
  featureText: {
    marginLeft: 12,
    fontSize: 15,
    color: colors.textPrimary,
  },
  input: {
    height: 120,
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    fontSize: 15,
    marginBottom: 16,
  },
  chipsContainer: {
    marginBottom: 0,
  },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    marginRight: 8,
    marginBottom: 4,
  },
  chipText: {
    fontSize: 14,
    fontWeight: '600',
  },
  resultContainer: {
    marginTop: 20,
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    paddingBottom: 40,
  },
  resultText: {
    fontSize: 15,
    lineHeight: 22,
  },
  versionText: {
    textAlign: 'center',
    color: colors.textSecondary,
    fontSize: 13,
    marginTop: 8,
  },
  // Onboarding styles
  onboardingContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  slide: {
    width: width,
    alignItems: 'center',
    paddingHorizontal: 40,
    paddingTop: 80,
  },
  slideIconContainer: {
    width: 120,
    height: 120,
    borderRadius: 60,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 40,
  },
  slideTitle: {
    fontSize: 28,
    fontWeight: '800',
    color: colors.textPrimary,
    textAlign: 'center',
    marginBottom: 16,
  },
  slideDescription: {
    fontSize: 17,
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 26,
  },
  pagination: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginTop: 40,
  },
  paginationDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: colors.cardBorder,
    marginHorizontal: 5,
  },
  paginationDotActive: {
    backgroundColor: colors.primary,
    width: 24,
  },
  onboardingButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingVertical: 40,
  },
  skipButton: {
    paddingVertical: 16,
    paddingHorizontal: 24,
  },
  skipButtonText: {
    color: colors.textSecondary,
    fontSize: 16,
    fontWeight: '600',
  },
  getStartedWrapper: {
    flex: 1,
    borderRadius: 14,
    overflow: 'hidden',
  },
  getStartedButton: {
    paddingVertical: 18,
    alignItems: 'center',
    borderRadius: 14,
    backgroundColor: colors.primary,
  },
  getStartedText: {
    color: colors.buttonText,
    fontSize: 18,
    fontWeight: '700',
  },
  // How to use styles
  howToStep: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 16,
  },
  howToIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.stepBg,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  howToContent: {
    flex: 1,
  },
  howToTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textPrimary,
    marginBottom: 4,
  },
  howToDesc: {
    fontSize: 14,
    color: colors.textSecondary,
    lineHeight: 20,
  },
});

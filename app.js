/**
 * JARVIS - Mobile AI Assistant Core Engine
 * Handled Features: State Management, Local Storage, Voice Input, Voice Output, Gemini 2.5 API integration
 */

// --- 1. State Configuration ---
const STATE = {
    isListening: false,
    isProcessing: false,
    isSpeaking: false,
    ttsEnabled: true,
    autoScrollEnabled: true,
    apiKey: localStorage.getItem('jarvis_api_key') || '',
    chatHistory: JSON.parse(localStorage.getItem('jarvis_chat_history')) || []
};

// --- 2. DOM Elements ---
const elements = {
    appContainer: document.querySelector('.app-container'),
    statusText: document.getElementById('status-text'),
    statusFeedback: document.getElementById('status-feedback'),
    ttsToggle: document.getElementById('tts-toggle'),
    clearBtn: document.getElementById('clear-btn'),
    settingsBtn: document.getElementById('settings-btn'),
    arcReactor: document.getElementById('arc-reactor-trigger'),
    chatConsole: document.getElementById('chat-console'),
    micBtn: document.getElementById('mic-btn'),
    chatInput: document.getElementById('chat-input'),
    sendBtn: document.getElementById('send-btn'),
    typingIndicator: document.getElementById('typing-indicator'),

    // Settings Modal
    settingsModal: document.getElementById('settings-modal'),
    closeSettings: document.getElementById('close-settings'),
    apiKeyInput: document.getElementById('api-key-input'),
    toggleKeyVisibility: document.getElementById('toggle-key-visibility'),
    voiceSelector: document.getElementById('voice-selector'),
    autoScrollToggle: document.getElementById('auto-scroll-toggle'),
    saveSettingsBtn: document.getElementById('save-settings-btn')
};

// --- 3. Speech APIs Setup ---
let recognition = null;
let speechSynth = window.speechSynthesis;
let synthUtterance = null;
let selectedVoice = null;

// Initialize Speech Recognition (Web Speech API)
function initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        console.warn("Speech Recognition API is not supported in this browser.");
        elements.statusFeedback.textContent = "VOICE INPUT UNSUPPORTED BY BROWSER";
        return;
    }

    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.lang = 'hi-IN';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
        STATE.isListening = true;
        updateArcReactorUI();
        elements.micBtn.classList.add('listening');
        elements.statusText.textContent = "LISTENING TO BOSS...";
        elements.statusFeedback.textContent = "I'M LISTENING, BOSS...";
    };

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        elements.chatInput.value = transcript;
        elements.statusFeedback.textContent = "TRANSCRIPTION COMPLETE";

        // Auto-send voice input
        setTimeout(() => {
            handleSendMessage();
        }, 500);
    };

    recognition.onerror = (event) => {
        console.error("Speech recognition error:", event.error);
        elements.statusFeedback.textContent = `VOICE ERR: ${event.error.toUpperCase()}`;
        stopListening();
    };

    recognition.onend = () => {
        stopListening();
    };
}

// Toggle Speech Recognition
function toggleListening() {
    if (!recognition) {
        initSpeechRecognition();
        if (!recognition) return;
    }

    if (STATE.isListening) {
        recognition.stop();
    } else {
        // Stop any playing speech output first
        stopSpeaking();
        try {
            recognition.start();
        } catch (e) {
            console.error(e);
        }
    }
}

function stopListening() {
    STATE.isListening = false;
    updateArcReactorUI();
    elements.micBtn.classList.remove('listening');
    elements.statusText.textContent = STATE.isProcessing ? "PROCESSING..." : "ONLINE - STANDBY";
    if (!STATE.isProcessing && !STATE.isSpeaking) {
        elements.statusFeedback.textContent = "TAP CORE OR MIC TO SPEAK";
    }
}

// Speech Synthesis (Text-to-Speech)
function speak(text) {
    if (!STATE.ttsEnabled || !speechSynth) return;

    // Cancel any active speech
    stopSpeaking();

    // Clean up markdown/extra characters from speech input
    const cleanSpeechText = text
        .replace(/```[\s\S]*?```/g, 'code block omitted') // skip code blocks
        .replace(/`([^`]+)`/g, '$1') // remove single backticks
        .replace(/[*#_\-]/g, ''); // remove basic markdown styling

    synthUtterance = new SpeechSynthesisUtterance(cleanSpeechText);

    // Assign selected voice if available
    if (selectedVoice) {
        synthUtterance.voice = selectedVoice;
    }

    synthUtterance.onstart = () => {
        STATE.isSpeaking = true;
        updateArcReactorUI();
        elements.statusText.textContent = "JARVIS SPEAKING...";
        elements.statusFeedback.textContent = "TRANSMITTING VOICE...";
    };

    synthUtterance.onend = () => {
        STATE.isSpeaking = false;
        updateArcReactorUI();
        elements.statusText.textContent = "ONLINE - STANDBY";
        elements.statusFeedback.textContent = "TAP CORE OR MIC TO SPEAK";
    };

    synthUtterance.onerror = (e) => {
        console.error("Speech synthesis error:", e);
        STATE.isSpeaking = false;
        updateArcReactorUI();
    };

    speechSynth.speak(synthUtterance);
}

function stopSpeaking() {
    if (speechSynth && speechSynth.speaking) {
        speechSynth.cancel();
    }
    STATE.isSpeaking = false;
    updateArcReactorUI();
}

// Populate System Voices List in Settings
function populateVoices() {
    if (!speechSynth) return;

    const voices = speechSynth.getVoices();
    elements.voiceSelector.innerHTML = '<option value="default">Default System Voice</option>';

    // Look for english, high-quality, or robot-like voices
    voices.forEach((voice, index) => {
        const option = document.createElement('option');
        option.value = index;
        option.textContent = `${voice.name} (${voice.lang})`;
        elements.voiceSelector.appendChild(option);
    });
}

// Handle voice changes inside SpeechSynth API
if (speechSynth) {
    if (speechSynth.onvoiceschanged !== undefined) {
        speechSynth.onvoiceschanged = populateVoices;
    }
}

// --- 4. Extensible Feature Router (Easy to add features later) ---
/**
 * CommandRouter detects specific action keywords in text and runs custom behavior.
 * This makes it incredibly easy for developers to add custom capabilities in the future.
 */
const CommandRouter = {
    commands: [],

    // Register a new extension or command
    register(name, keywords, callback) {
        this.commands.push({ name, keywords, callback });
        console.log(`[Jarvis System] Registered extension: "${name}"`);
    },

    // Check query for registered commands
    route(query) {
        const lowerQuery = query.toLowerCase();
        for (const cmd of this.commands) {
            for (const kw of cmd.keywords) {
                if (lowerQuery.includes(kw.toLowerCase())) {
                    return cmd.callback(query);
                }
            }
        }
        return null; // Let standard pipeline handle it
    }
};

// --- Register Custom Plug-in Features ---
// 1. Flashlight (Mock hardware action)
CommandRouter.register("Flashlight Action", ["flashlight", "torch"], (query) => {
    const isOff = query.toLowerCase().includes("off") || query.toLowerCase().includes("disable") || query.toLowerCase().includes("stop");
    const reply = isOff
        ? "Yes Boss. Disabling mobile flashlight matrix immediately."
        : "Yes Boss. Activating high-intensity flashlight beam.";
    return {
        replyText: reply,
        extraUI: `<div class="system-message"><span class="timestamp">HARDWARE INTERFACE</span><p>${isOff ? "FLASHLIGHT POWER: DEACTIVATED" : "FLASHLIGHT POWER: ENGAGED [100%]"}</p></div>`
    };
});

// 2. Music Player (Mock device music action)
CommandRouter.register("Music Controller", ["play music", "play song", "stop music"], (query) => {
    const isStop = query.toLowerCase().includes("stop");
    const reply = isStop
        ? "Stopping sound wave projection. Audio systems placed on standby, Boss."
        : "Initializing holographic audio deck. Playing your standard playlist on YouTube/Music streams.";
    return {
        replyText: reply,
        extraUI: `<div class="system-message"><span class="timestamp">AUDIO SYSTEM</span><p>${isStop ? "STATUS: MUTED" : "STATUS: PLAYING 'ROSHAN ROHI SONG'"}</p></div>`
    };
});

// 3. System Reboot
CommandRouter.register("System Reboot", ["system reboot", "restart core"], () => {
    return {
        replyText: "Initiating Jarvis reactor reboot. Cycling all subsystems. Please standby, Boss.",
        extraUI: `<div class="system-message"><span class="timestamp">SYS INTEGRITY</span><p>REBOOTING SYSTEM CORES... ACTIVE</p></div>`
    };
});


// --- 5. Core NLP & Conversation Pipeline ---

// Render and append chat messages
function appendMessage(sender, text, timestampText = null) {
    const timestamp = timestampText || new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;

    const metaSpan = document.createElement('span');
    metaSpan.className = 'message-meta';

    const timeSpan = document.createElement('span');
    timeSpan.className = 'timestamp';
    timeSpan.textContent = timestamp;

    if (sender === 'user') {
        metaSpan.textContent = 'BOSS';
        metaSpan.appendChild(timeSpan);
    } else {
        metaSpan.textContent = 'JARVIS';
        metaSpan.prepend(timeSpan);
    }

    const bubbleDiv = document.createElement('div');
    bubbleDiv.className = 'message-bubble';

    // Format markdown-like code tags dynamically for tech responses
    bubbleDiv.innerHTML = formatMessageText(text);

    messageDiv.appendChild(metaSpan);
    messageDiv.appendChild(bubbleDiv);
    elements.chatConsole.appendChild(messageDiv);

    if (STATE.autoScrollEnabled) {
        elements.chatConsole.scrollTop = elements.chatConsole.scrollHeight;
    }
}

// Format basic markdown/tech elements
function formatMessageText(text) {
    // Escape standard HTML first to prevent XSS
    let escaped = text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

    // Replace code blocks: ```code```
    escaped = escaped.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');

    // Replace inline code tags: `code`
    escaped = escaped.replace(/`([^`\n]+)`/g, '<code>$1</code>');

    // Replace linebreaks
    return escaped.replace(/\n/g, '<br>');
}

// Core Message Processing
async function processCommand(query) {
    const trimmed = query.trim();
    if (!trimmed) return;

    // Stop speaking currently active text
    stopSpeaking();

    // Add User message
    appendMessage('user', trimmed);
    STATE.chatHistory.push({ sender: 'user', text: trimmed, timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
    saveChatHistory();

    // 1. Direct Pattern Match: "Hello Jarvis" (Required)
    const lower = trimmed.toLowerCase();
    if (lower === "hello jarvis" || lower === "hello, jarvis" || lower === "hey jarvis" || lower === "hi jarvis") {
        const reply = "Yes Boss, how can I help you?";
        setTimeout(() => {
            appendMessage('jarvis', reply);
            STATE.chatHistory.push({ sender: 'jarvis', text: reply, timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
            saveChatHistory();
            speak(reply);
        }, 400);
        return;
    }

    // 2. Custom Extensions / Commands Checking
    const extensionResult = CommandRouter.route(trimmed);
    if (extensionResult) {
        STATE.isProcessing = true;
        updateArcReactorUI();
        elements.typingIndicator.style.display = 'flex';

        setTimeout(() => {
            elements.typingIndicator.style.display = 'none';
            STATE.isProcessing = false;
            updateArcReactorUI();

            appendMessage('jarvis', extensionResult.replyText);
            STATE.chatHistory.push({ sender: 'jarvis', text: extensionResult.replyText, timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
            saveChatHistory();

            if (extensionResult.extraUI) {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = extensionResult.extraUI;
                elements.chatConsole.appendChild(tempDiv.firstChild);
            }

            speak(extensionResult.replyText);
        }, 600);
        return;
    }

    // 3. Gemini API Chat integration
    STATE.isProcessing = true;
    updateArcReactorUI();
    elements.typingIndicator.style.display = 'flex';
    elements.statusText.textContent = "PROCESSING COMMAND...";
    elements.statusFeedback.textContent = "CONSULTING NEURAL MATRIX...";

    try {
        const reply = await callGeminiApi(trimmed);

        elements.typingIndicator.style.display = 'none';
        STATE.isProcessing = false;
        updateArcReactorUI();

        appendMessage('jarvis', reply);
        STATE.chatHistory.push({ sender: 'jarvis', text: reply, timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
        saveChatHistory();

        speak(reply);
    } catch (error) {
        console.error("Pipeline failure:", error);
        elements.typingIndicator.style.display = 'none';
        STATE.isProcessing = false;
        updateArcReactorUI();

        const fallbackReply = "Deepest apologies Boss, my cloud uplink experienced a fluctuation. What can I do for you?";
        appendMessage('jarvis', fallbackReply);
        speak(fallbackReply);
    }
}

// Gemini API Integration Call
async function callGeminiApi(prompt) {
    if (!STATE.apiKey || STATE.apiKey === 'MY_GEMINI_API_KEY') {
        return "I am at your service, Boss. However, my connection to the Gemini cloud is currently offline. Please open the system settings panel in the top right to configure your Gemini API Key.";
    }

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${STATE.apiKey}`;

    const systemInstruction = `
        You are Jarvis, the legendary AI assistant. Your name is Jarvis.
        You must always address the user as "Boss".
        Your response style is polite, exceptionally helpful, efficient, and technically sophisticated.
        Keep answers clear, highly structured, and concise unless the Boss specifically requests a comprehensive layout or deep analysis.
    `;

    // Package last few history items for basic context
    const recentHistory = STATE.chatHistory.slice(-6).map(msg => ({
        role: msg.sender === 'user' ? 'user' : 'model',
        parts: [{ text: msg.text }]
    }));

    // If history doesn't exist yet, push the current prompt
    if (recentHistory.length === 0 || recentHistory[recentHistory.length - 1].parts[0].text !== prompt) {
        recentHistory.push({
            role: 'user',
            parts: [{ text: prompt }]
        });
    }

    const jsonBody = {
        systemInstruction: {
            parts: [{ text: systemInstruction }]
        },
        contents: recentHistory
    };

    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(jsonBody)
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error("Gemini API Error details:", errorData);
        throw new Error(`Gemini API responded with status ${response.status}`);
    }

    const root = await response.json();
    const text = root?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) {
        throw new Error("No response parts received from Gemini API");
    }

    return text.trim();
}


// --- 6. Helper Utilities ---

function updateArcReactorUI() {
    const reactor = elements.arcReactor;
    reactor.classList.remove('listening', 'processing', 'speaking');

    if (STATE.isListening) {
        reactor.classList.add('listening');
    } else if (STATE.isProcessing) {
        reactor.classList.add('processing');
    } else if (STATE.isSpeaking) {
        reactor.classList.add('speaking');
    }
}

function handleSendMessage() {
    const query = elements.chatInput.value;
    if (!query.trim()) return;

    elements.chatInput.value = '';
    processCommand(query);
}

function saveChatHistory() {
    localStorage.setItem('jarvis_chat_history', JSON.stringify(STATE.chatHistory));
}

function loadSavedChatHistory() {
    const history = STATE.chatHistory;
    if (history.length > 0) {
        // Clear default connection log
        elements.chatConsole.innerHTML = '';
        history.forEach(msg => {
            appendMessage(msg.sender, msg.text, msg.timestamp);
        });
    }
}

// --- 7. Event Binding & Initialization ---

function initEventListeners() {
    // Send Command Event
    elements.sendBtn.addEventListener('click', handleSendMessage);
    elements.chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            handleSendMessage();
        }
    });

    // Mic Click Event
    elements.micBtn.addEventListener('click', toggleListening);

    // Arc Reactor Central Interactive Toggle
    elements.arcReactor.addEventListener('click', () => {
        if (STATE.isListening || STATE.isProcessing || STATE.isSpeaking) {
            // Cancel current states
            if (STATE.isListening) stopListening();
            if (STATE.isSpeaking) stopSpeaking();
        } else {
            toggleListening();
        }
    });

    // Top Bar Control Events
    elements.ttsToggle.addEventListener('click', () => {
        STATE.ttsEnabled = !STATE.ttsEnabled;
        if (!STATE.ttsEnabled) {
            stopSpeaking();
            elements.ttsToggle.classList.remove('active');
            elements.ttsToggle.querySelector('span').textContent = 'volume_off';
            elements.statusFeedback.textContent = "SPEECH MUTED";
        } else {
            elements.ttsToggle.classList.add('active');
            elements.ttsToggle.querySelector('span').textContent = 'volume_up';
            elements.statusFeedback.textContent = "SPEECH ENABLED";
            speak("Voice synthesizer engaged, Boss.");
        }
    });

    elements.clearBtn.addEventListener('click', () => {
        if (confirm("Confirm erasing all memory cores, Boss?")) {
            stopSpeaking();
            stopListening();
            STATE.chatHistory = [];
            localStorage.removeItem('jarvis_chat_history');
            elements.chatConsole.innerHTML = `
                <div class="system-message">
                    <span class="timestamp">MEM ERASED</span>
                    <p>Subsystem memory cleared successfully. System placed on standby.</p>
                </div>
            `;
            elements.statusFeedback.textContent = "MEMORY ERASED";
            speak("Subsystem memories cleared, Boss.");
        }
    });

    // Settings Modal Toggles
    elements.settingsBtn.addEventListener('click', () => {
        elements.apiKeyInput.value = STATE.apiKey;
        elements.autoScrollToggle.checked = STATE.autoScrollEnabled;
        populateVoices();

        // Show saved voice if exists
        const savedVoiceIndex = localStorage.getItem('jarvis_selected_voice_idx');
        if (savedVoiceIndex !== null && elements.voiceSelector.children[savedVoiceIndex]) {
            elements.voiceSelector.value = savedVoiceIndex;
        }

        elements.settingsModal.classList.add('open');
    });

    elements.closeSettings.addEventListener('click', () => {
        elements.settingsModal.classList.remove('open');
    });

    elements.toggleKeyVisibility.addEventListener('click', () => {
        const input = elements.apiKeyInput;
        const icon = elements.toggleKeyVisibility.querySelector('span');
        if (input.type === 'password') {
            input.type = 'text';
            icon.textContent = 'visibility_off';
        } else {
            input.type = 'password';
            icon.textContent = 'visibility';
        }
    });

    elements.saveSettingsBtn.addEventListener('click', () => {
        const newKey = elements.apiKeyInput.value.trim();
        STATE.apiKey = newKey;
        localStorage.setItem('jarvis_api_key', newKey);

        STATE.autoScrollEnabled = elements.autoScrollToggle.checked;

        // Save selected voice
        const voiceVal = elements.voiceSelector.value;
        if (voiceVal !== 'default' && speechSynth) {
            const voices = speechSynth.getVoices();
            selectedVoice = voices[parseInt(voiceVal)];
            localStorage.setItem('jarvis_selected_voice_idx', voiceVal);
        } else {
            selectedVoice = null;
            localStorage.removeItem('jarvis_selected_voice_idx');
        }

        elements.settingsModal.classList.remove('open');
        elements.statusFeedback.textContent = "CONFIG APPLIED";
        speak("System configuration synchronized, Boss.");
    });

    // Click on Quick Preset Chips
    document.querySelectorAll('.preset-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const cmd = chip.getAttribute('data-cmd');
            elements.chatInput.value = cmd;
            handleSendMessage();
        });
    });

    // Close settings if user clicks outside modal content
    elements.settingsModal.addEventListener('click', (e) => {
        if (e.target === elements.settingsModal) {
            elements.settingsModal.classList.remove('open');
        }
    });
}

// --- 8. Initialize System ---
function init() {
    // Initial UI State setup
    if (STATE.ttsEnabled) {
        elements.ttsToggle.classList.add('active');
    } else {
        elements.ttsToggle.querySelector('span').textContent = 'volume_off';
    }

    // Try pre-loading selected voice
    if (speechSynth) {
        setTimeout(() => {
            populateVoices();
            const savedVoiceIndex = localStorage.getItem('jarvis_selected_voice_idx');
            if (savedVoiceIndex !== null) {
                const voices = speechSynth.getVoices();
                if (voices[parseInt(savedVoiceIndex)]) {
                    selectedVoice = voices[parseInt(savedVoiceIndex)];
                }
            }
        }, 500);
    }

    initSpeechRecognition();
    initEventListeners();
    loadSavedChatHistory();

    console.log("[Jarvis System] Subsystems online. Welcome back, Boss.");
}

// Start core once DOM loads
document.addEventListener('DOMContentLoaded', init);

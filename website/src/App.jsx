import React, { useState, useEffect } from 'react';
import {
  Menu, X, Github, Monitor, Smartphone, Wifi, Shield, Server, Activity,
  ArrowRight, Check, Play, AppWindow, Cpu, Key, RefreshCw, Terminal,
  CheckCircle2, ChevronDown, Download, HelpCircle, HardDrive, Info,
  Globe, ShieldAlert, Zap, Layers, Copy, FileText, MousePointer, Volume2, Power
} from 'lucide-react';

export default function App() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [activeExperienceTab, setActiveExperienceTab] = useState('trackpad');
  const [openFaqIndex, setOpenFaqIndex] = useState(null);

  // Showcase Experience Interactive States
  const [trackpadClicks, setTrackpadClicks] = useState({ left: false, right: false });
  const [zoomLevel, setZoomLevel] = useState(100);
  const [streamQuality, setStreamQuality] = useState('HIGH');
  const [cpuUsage, setCpuUsage] = useState(34);
  const [ramUsage, setRamUsage] = useState(58);
  const [uptime, setUptime] = useState('02h 41m 15s');
  const [launchedApp, setLaunchedApp] = useState(null);
  const [discoveryStep, setDiscoveryStep] = useState('mdns');

  // Simulate changing telemetry
  useEffect(() => {
    const timer = setInterval(() => {
      setCpuUsage(prev => {
        const diff = Math.floor(Math.random() * 11) - 5; // -5 to +5
        const next = prev + diff;
        return Math.max(15, Math.min(85, next));
      });
      setRamUsage(prev => {
        const diff = Math.floor(Math.random() * 5) - 2; // -2 to +2
        const next = prev + diff;
        return Math.max(50, Math.min(65, next));
      });

      // Update uptime seconds
      setUptime(prev => {
        const parts = prev.split(' ').map(p => parseInt(p));
        let s = parts[2] + 1;
        let m = parts[1];
        let h = parts[0];
        if (s >= 60) {
          s = 0;
          m += 1;
        }
        if (m >= 60) {
          m = 0;
          h += 1;
        }
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(h)}h ${pad(m)}m ${pad(s)}s`;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const triggerClick = (side) => {
    setTrackpadClicks(prev => ({ ...prev, [side]: true }));
    setTimeout(() => {
      setTrackpadClicks(prev => ({ ...prev, [side]: false }));
    }, 150);
  };

  const handleLaunchApp = (appName) => {
    setLaunchedApp(appName);
    setTimeout(() => {
      setLaunchedApp(null);
    }, 2500);
  };

  const faqs = [
    {
      q: "Why doesn't my PC appear automatically in the client list?",
      a: "This is usually caused by network multicast blocking or Windows Firewall. Make sure both devices are on the exact same Wi-Fi / LAN network (not a Guest network with client isolation). Also, verify that you allowed inbound access for ports 5000, 5001, and 5002 when installing. If mDNS fails, the client automatically attempts a parallel subnet unicast probe. If all fails, you can tap 'Enter IP Manually' to connect directly using your PC's IP address."
    },
    {
      q: "Can I connect manually if my router blocks all discovery?",
      a: "Yes! Simply right-click the Nexa Remote icon in your Windows system tray, copy the shown local IPv4 address (e.g., 192.168.1.15), then tap 'Enter IP Manually' on your Android device, type the IP, and hit connect."
    },
    {
      q: "What happens if my PC's local IP address changes?",
      a: "Nexa Remote's automatic discovery protocol handles this gracefully! The next time you open the app, it will scan for the server's unique mDNS service identifier and re-resolve the new IP address dynamically. Your previously authorized cryptographic pairing token remains valid."
    },
    {
      q: "Does Nexa Remote require an internet connection?",
      a: "Absolutely not! Nexa Remote operates 100% locally on your home or office network. It sends zero packets to the internet, making it incredibly fast, private, and secure against external network outages."
    }
  ];

  return (
    <div className="relative min-h-screen bg-zinc-950 text-zinc-100 bg-grid-cyan overflow-x-hidden">

      {/* 1. NAVBAR */}
      <nav className="sticky top-0 z-50 backdrop-blur-md bg-zinc-950/80 border-b border-zinc-900 transition-all duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">

            {/* Logo */}
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/10">
                <Smartphone className="w-5 h-5 text-white stroke-[2]" />
              </div>
              <div>
                <span className="font-extrabold text-xl tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-zinc-100 via-zinc-200 to-cyan-400">
                  Nexa <span className="text-cyan-400">Remote</span>
                </span>
                <span className="ml-1.5 px-1.5 py-0.5 text-[9px] font-bold rounded bg-zinc-800 border border-zinc-700 text-zinc-400">V1.0.1</span>
              </div>
            </div>

            {/* Desktop Navigation Links */}
            <div className="hidden md:flex items-center gap-8">
              <a href="#features" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Features</a>
              <a href="#how-it-works" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Architecture</a>
              <a href="#discovery" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Discovery</a>
              <a href="#security" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Security</a>
              <a href="#showcase" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Live Experience</a>
              <a href="#install" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">Installation</a>
              <a href="#faq" className="text-sm font-medium text-zinc-400 hover:text-cyan-400 transition-colors">FAQ</a>
            </div>

            {/* GitHub Action */}
            <div className="hidden md:flex items-center gap-3">
              <a
                href="https://github.com/Srinivasrao422/Nexa-Remote"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 px-4 py-2 text-sm font-medium bg-zinc-900 border border-zinc-800 rounded-xl hover:bg-zinc-850 hover:border-zinc-700 text-zinc-300 hover:text-white transition-all duration-200"
              >
                <Github className="w-4 h-4" />
                <span>Star on GitHub</span>
              </a>
            </div>

            {/* Mobile Hamburger Trigger */}
            <div className="md:hidden">
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="p-2 text-zinc-400 hover:text-white focus:outline-none"
              >
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation Dropdown */}
        {mobileMenuOpen && (
          <div className="md:hidden border-t border-zinc-900 bg-zinc-950/95 backdrop-blur-lg">
            <div className="px-2 pt-2 pb-4 space-y-1 sm:px-3">
              <a
                href="#features"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Features
              </a>
              <a
                href="#how-it-works"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Architecture
              </a>
              <a
                href="#discovery"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Discovery
              </a>
              <a
                href="#security"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Security
              </a>
              <a
                href="#showcase"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Live Experience
              </a>
              <a
                href="#install"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                Installation
              </a>
              <a
                href="#faq"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-base font-medium text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg"
              >
                FAQ
              </a>
              <div className="pt-4 pb-2 border-t border-zinc-900 px-3 flex flex-col gap-2">
                <a
                  href="https://github.com/Srinivasrao422/Nexa-Remote"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-center gap-2 px-4 py-2.5 bg-zinc-900 border border-zinc-800 rounded-xl hover:bg-zinc-800 hover:border-zinc-700 text-zinc-300"
                >
                  <Github className="w-5 h-5" />
                  <span>Star on GitHub</span>
                </a>
              </div>
            </div>
          </div>
        )}
      </nav>

      {/* 2. HERO SECTION */}
      <section className="relative pt-12 pb-24 md:pt-20 md:pb-32 overflow-hidden">
        {/* Abstract background blobs */}
        <div className="absolute top-1/4 -left-1/4 w-[600px] h-[600px] rounded-full bg-cyan-900/10 blur-[120px] pointer-events-none" />
        <div className="absolute bottom-10 -right-1/4 w-[500px] h-[500px] rounded-full bg-blue-900/10 blur-[150px] pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">

            {/* Hero text */}
            <div className="lg:col-span-7 flex flex-col items-center lg:items-start text-center lg:text-left">

              {/* Pulse Badge */}
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-cyan-950/50 border border-cyan-800/50 text-cyan-400 font-medium text-xs mb-6 animate-pulse-slow">
                <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
                Latest Stable Release &middot; V1.0.1
              </div>

              {/* Title */}
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-white leading-[1.1] mb-6">
                Your PC, <br className="hidden sm:inline" />
                <span className="bg-clip-text text-transparent bg-gradient-to-r from-cyan-400 via-teal-300 to-blue-500">
                  remotely connected.
                </span>
              </h1>

              {/* Description */}
              <p className="text-lg text-zinc-400 max-w-xl leading-relaxed mb-8">
                Control your Windows PC from your Android device over your local network — fast, private, and built for your own devices. No subscriptions, no cloud relays, 100% open-source.
              </p>

              {/* CTAs */}
              <div className="flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
                <a
                  href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.1.0.1.apk"
                  className="flex items-center justify-center gap-2 px-6 py-3.5 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-semibold rounded-xl shadow-lg shadow-cyan-500/20 hover:shadow-cyan-400/30 transition-all duration-200"
                >
                  <Smartphone className="w-5 h-5" />
                  Download for Android
                </a>

                <a
                  href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.Setup.exe"
                  className="flex items-center justify-center gap-2 px-6 py-3.5 bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 hover:border-zinc-700 text-zinc-200 hover:text-white font-semibold rounded-xl transition-all duration-200"
                >
                  <Monitor className="w-5 h-5" />
                  Download for Windows
                </a>
              </div>

              <div className="mt-4 text-xs text-zinc-500">
                Official releases cryptographically signed. See official V1.0.1 release notes on{' '}
                <a href="https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1" target="_blank" rel="noopener noreferrer" className="text-cyan-500 hover:underline">GitHub</a>.
              </div>
            </div>

            {/* Premium Interactive Mockup */}
            <div className="lg:col-span-5 flex justify-center items-center relative">
              <div className="absolute inset-0 bg-gradient-to-tr from-cyan-500/10 to-blue-500/10 rounded-full blur-[80px]" />

              <div className="relative w-full max-w-[420px] aspect-square flex items-center justify-center">

                {/* Simulated Windows Monitor (Background-left) */}
                <div className="absolute top-1/10 left-1/12 w-[280px] h-[190px] bg-zinc-900/90 border border-zinc-800 rounded-xl shadow-2xl p-3 z-10 hidden sm:block backdrop-blur-md">
                  <div className="flex items-center justify-between border-b border-zinc-800 pb-1.5 mb-2">
                    <div className="flex items-center gap-1.5">
                      <div className="w-2.5 h-2.5 rounded-full bg-red-500/60" />
                      <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/60" />
                      <div className="w-2.5 h-2.5 rounded-full bg-green-500/60" />
                    </div>
                    <span className="text-[9px] text-zinc-500 font-mono">Nexa Server - Windows 11</span>
                  </div>
                  {/* PC Telemetry Mockup */}
                  <div className="space-y-2 font-mono text-[9px]">
                    <div className="p-1.5 bg-zinc-950/80 rounded border border-zinc-850 flex items-center justify-between">
                      <span className="text-cyan-400">Status</span>
                      <span className="text-emerald-400 flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                        Listening
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-1.5">
                      <div className="p-1.5 bg-zinc-950/80 rounded border border-zinc-850">
                        <span className="text-zinc-500 block">CPU</span>
                        <span className="text-zinc-200 font-bold">{cpuUsage}%</span>
                      </div>
                      <div className="p-1.5 bg-zinc-950/80 rounded border border-zinc-850">
                        <span className="text-zinc-500 block">RAM</span>
                        <span className="text-zinc-200 font-bold">{ramUsage}%</span>
                      </div>
                    </div>
                    <div className="p-1 text-[8px] text-zinc-600 truncate">
                      IP: 192.168.1.104 | Active Ports: 3
                    </div>
                  </div>
                </div>

                {/* Simulated Android Phone (Foreground-right) */}
                <div className="absolute bottom-1/10 right-1/12 w-[180px] h-[340px] bg-zinc-950 border-4 border-zinc-800 rounded-[32px] shadow-2xl overflow-hidden z-20 flex flex-col justify-between p-3.5">
                  {/* Phone Speaker/Camera notch */}
                  <div className="absolute top-1.5 left-1/2 -translate-x-1/2 w-16 h-3.5 bg-zinc-800 rounded-full flex items-center justify-center">
                    <span className="w-1.5 h-1.5 rounded-full bg-zinc-950" />
                  </div>

                  {/* App Header */}
                  <div className="pt-2 flex items-center justify-between border-b border-zinc-900 pb-2">
                    <div className="flex items-center gap-1">
                      <div className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                      <span className="text-[9px] font-bold text-zinc-200">Connected</span>
                    </div>
                    <Wifi className="w-3.5 h-3.5 text-cyan-400 animate-pulse" />
                  </div>

                  {/* Remote Trackpad Mockup inside app */}
                  <div className="flex-1 my-3 bg-zinc-900/60 rounded-xl border border-zinc-850 flex flex-col items-center justify-center p-2 relative group overflow-hidden">
                    <div className="absolute inset-0 bg-radial-gradient from-cyan-500/5 to-transparent pointer-events-none" />
                    <MousePointer className="w-6 h-6 text-cyan-500 mb-1 animate-bounce" />
                    <span className="text-[8px] text-zinc-400 text-center font-medium font-sans">
                      Virtual Trackpad
                    </span>
                    <div className="absolute bottom-1.5 left-1.5 right-1.5 grid grid-cols-2 gap-1">
                      <div className="h-6 rounded bg-zinc-800/80 border border-zinc-700/50 flex items-center justify-center text-[7px] text-zinc-400 font-bold">L-Click</div>
                      <div className="h-6 rounded bg-zinc-800/80 border border-zinc-700/50 flex items-center justify-center text-[7px] text-zinc-400 font-bold">R-Click</div>
                    </div>
                  </div>

                  {/* Client App bottom bar */}
                  <div className="flex items-center justify-around bg-zinc-900/90 py-1.5 px-1 rounded-lg border border-zinc-850 text-[8px] text-zinc-400">
                    <span className="text-cyan-400 font-bold">Mouse</span>
                    <span>Keys</span>
                    <span>Screen</span>
                  </div>
                </div>

                {/* Connectivity Wave Rings (Animations between monitor and phone) */}
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[340px] h-[340px] pointer-events-none">
                  <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[240px] h-[240px] rounded-full border border-dashed border-cyan-500/10 animate-spin" style={{ animationDuration: '40s' }} />
                  <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[160px] h-[160px] rounded-full border border-dashed border-cyan-500/20 animate-spin" style={{ animationDuration: '15s', animationDirection: 'reverse' }} />
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 3. TRUST / PRODUCT STRIP */}
      <section className="border-y border-zinc-900 bg-zinc-950/60 backdrop-blur-md relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
          <div className="grid grid-cols-2 md:grid-cols-5 gap-6 text-center">

            <div className="flex flex-col items-center">
              <div className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-800 mb-2">
                <Globe className="w-5 h-5 text-cyan-400" />
              </div>
              <span className="text-sm font-semibold text-zinc-100">Local Network Only</span>
              <span className="text-xs text-zinc-500 mt-1">Direct device communication</span>
            </div>

            <div className="flex flex-col items-center">
              <div className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-800 mb-2">
                <Shield className="w-5 h-5 text-emerald-400" />
              </div>
              <span className="text-sm font-semibold text-zinc-100">No Cloud Relay</span>
              <span className="text-xs text-zinc-500 mt-1">Zero internet required</span>
            </div>

            <div className="flex flex-col items-center">
              <div className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-800 mb-2">
                <Github className="w-5 h-5 text-purple-400" />
              </div>
              <span className="text-sm font-semibold text-zinc-100">Open Source (MIT)</span>
              <span className="text-xs text-zinc-500 mt-1">Transparent codebase</span>
            </div>

            <div className="flex flex-col items-center">
              <div className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-800 mb-2">
                <Wifi className="w-5 h-5 text-blue-400" />
              </div>
              <span className="text-sm font-semibold text-zinc-100">Direct Wi-Fi / LAN</span>
              <span className="text-xs text-zinc-500 mt-1">Ultra low latency response</span>
            </div>

            <div className="flex flex-col items-center col-span-2 md:col-span-1">
              <div className="p-2.5 rounded-xl bg-zinc-900 border border-zinc-800 mb-2">
                <Monitor className="w-5 h-5 text-orange-400" />
              </div>
              <span className="text-sm font-semibold text-zinc-100">Android + Windows</span>
              <span className="text-xs text-zinc-500 mt-1">Seamless cross-platform</span>
            </div>

          </div>
        </div>
      </section>

      {/* 4. FEATURES GRID */}
      <section id="features" className="py-24 relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
              Powerfully local. Feature rich.
            </h2>
            <p className="text-zinc-400 text-lg">
              Every tool you need to manage and control your desktop from your phone, without compromising speed or privacy.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">

            {/* Feature 1 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-cyan-950 border border-cyan-900/50 flex items-center justify-center mb-4">
                <MousePointer className="w-5 h-5 text-cyan-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Remote Mouse & Touchpad</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Full desktop cursor control with smooth scrolling, left/right clicks, and highly responsive click-tap gestures.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-blue-950 border border-blue-900/50 flex items-center justify-center mb-4">
                <Terminal className="w-5 h-5 text-blue-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Keyboard & Shortcuts</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Full-featured virtual keyboard supporting Windows special modifiers (Win, Alt, Ctrl, Delete) and custom macros.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-emerald-950 border border-emerald-900/50 flex items-center justify-center mb-4">
                <Monitor className="w-5 h-5 text-emerald-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Live Screen Streaming</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Stream your PC screen to Android in real-time. Pinch to zoom, pan, and tap screen coordinates directly.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-purple-950 border border-purple-900/50 flex items-center justify-center mb-4">
                <Copy className="w-5 h-5 text-purple-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Clipboard Synchronization</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Instantly synchronize copied text between your Android device and Windows PC clipboard buffers with one click.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-orange-950 border border-orange-900/50 flex items-center justify-center mb-4">
                <FileText className="w-5 h-5 text-orange-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Local File Transfer</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Direct binary transfer. Upload photos from Android to PC or download files to your phone over safe local Wi-Fi.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-red-950 border border-red-900/50 flex items-center justify-center mb-4">
                <Power className="w-5 h-5 text-red-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">System Power Controls</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Securely execute standard Windows power commands: Shutdown, Restart, Sleep, Lock desktop, or Sign Out.
              </p>
            </div>

            {/* Feature 7 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-yellow-950 border border-yellow-900/50 flex items-center justify-center mb-4">
                <Volume2 className="w-5 h-5 text-yellow-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Quick Volume & Media</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Adjust PC system volume, mute settings, and control active media playback (Play, Pause, Next, Previous).
              </p>
            </div>

            {/* Feature 8 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-zinc-850 border border-zinc-800 flex items-center justify-center mb-4">
                <Activity className="w-5 h-5 text-zinc-300" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">PC Telemetry Dashboard</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Keep track of your computer's health: live CPU load, memory utilization, disk space, and system uptime.
              </p>
            </div>

            {/* Feature 9 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-rose-950 border border-rose-900/50 flex items-center justify-center mb-4">
                <AppWindow className="w-5 h-5 text-rose-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">App Launcher Allowlist</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Launch authorized Windows programs with a single click (Chrome, Edge, Spotify, VS Code, Task Manager).
              </p>
            </div>

            {/* Feature 10 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-teal-950 border border-teal-900/50 flex items-center justify-center mb-4">
                <Wifi className="w-5 h-5 text-teal-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Automatic PC Discovery</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Hassle-free pairing via mDNS / DNS-SD and a robust local-subnet parallel unicast fallback search protocol.
              </p>
            </div>

            {/* Feature 11 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-sky-950 border border-sky-900/50 flex items-center justify-center mb-4">
                <Key className="w-5 h-5 text-sky-400" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Secure Device Pairing</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                6-digit randomized PIN checks block unauthorized users. Persistent token keys are saved securely in Android Keystore.
              </p>
            </div>

            {/* Feature 12 */}
            <div className="p-6 rounded-2xl bg-zinc-900/40 border border-zinc-900 hover:border-zinc-800 transition-all duration-300 hover:translate-y-[-2px]">
              <div className="w-10 h-10 rounded-xl bg-violet-950 border border-violet-900/50 flex items-center justify-center mb-4">
                <RefreshCw className="w-5 h-5 text-violet-400 animate-pulse-slow" />
              </div>
              <h3 className="text-base font-bold text-white mb-2">Auto-Reconnect</h3>
              <p className="text-sm text-zinc-400 leading-relaxed">
                Recovers instantly if network dropouts occur or when switching between local routers and access points.
              </p>
            </div>

          </div>
        </div>
      </section>

      {/* 5. HOW IT WORKS (Technical Architecture) */}
      <section id="how-it-works" className="py-24 border-t border-zinc-900 bg-zinc-900/10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="text-center max-w-3xl mx-auto mb-16">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-zinc-900 border border-zinc-800 text-xs text-zinc-400 font-medium mb-3">
              <Layers className="w-3.5 h-3.5 text-cyan-400" />
              Multi-Socket Architecture
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
              Under the Hood: Three TCP Channels
            </h2>
            <p className="text-zinc-400 text-lg">
              To keep actions lag-free and stable, Nexa Remote routes commands, screen frames, and files across distinct high-speed TCP sockets.
            </p>
          </div>

          {/* Interactive Step Chart / Visual flow */}
          <div className="bg-zinc-950/80 rounded-3xl border border-zinc-900 p-8 lg:p-12 shadow-2xl relative">
            <div className="absolute top-0 right-0 bg-gradient-to-l from-cyan-500/5 to-transparent w-full h-full rounded-3xl pointer-events-none" />

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">

              {/* Architecture Steps description */}
              <div className="lg:col-span-5 space-y-6">

                <div className="border-l-2 border-cyan-500 pl-4 py-1">
                  <h4 className="text-base font-bold text-white">TCP Port 5000: Control Channel</h4>
                  <p className="text-sm text-zinc-400 mt-1">
                    The control spine. Carries pairing handshakes, token validations, mouse coordinates, keyboard inputs, and live PC hardware diagnostics.
                  </p>
                </div>

                <div className="border-l-2 border-blue-500 pl-4 py-1">
                  <h4 className="text-base font-bold text-white">TCP Port 5001: Streaming Channel</h4>
                  <p className="text-sm text-zinc-400 mt-1">
                    Delivers highly-optimized, low-latency JPEG buffer updates from the PC to your smartphone, adapting automatically to Wi-Fi bandwidth limits.
                  </p>
                </div>

                <div className="border-l-2 border-orange-500 pl-4 py-1">
                  <h4 className="text-base font-bold text-white">TCP Port 5002: Transfer Channel</h4>
                  <p className="text-sm text-zinc-400 mt-1">
                    A raw binary streaming pipe dedicated exclusively to safe upload and download transfers, leaving input commands completely unaffected by background loads.
                  </p>
                </div>

              </div>

              {/* Graphical representation */}
              <div className="lg:col-span-7 flex flex-col justify-center gap-4 bg-zinc-900/40 border border-zinc-850 p-6 rounded-2xl">

                {/* Node: Client */}
                <div className="flex items-center justify-between bg-zinc-950 border border-zinc-800 p-3.5 rounded-xl">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-cyan-950/50 border border-cyan-900 flex items-center justify-center">
                      <Smartphone className="w-4 h-4 text-cyan-400" />
                    </div>
                    <div>
                      <span className="block text-xs font-bold text-zinc-200">Android Client App</span>
                      <span className="block text-[10px] text-zinc-500">Android 7.0 (API 24+)</span>
                    </div>
                  </div>
                  <span className="text-[10px] font-mono bg-zinc-900 px-2 py-0.5 rounded border border-zinc-800 text-cyan-400">TX Sockets</span>
                </div>

                {/* Connectors (The three channels) */}
                <div className="py-2 space-y-3 relative">

                  {/* Channel 1 */}
                  <div className="flex items-center justify-between text-xs px-4">
                    <span className="font-mono text-[10px] font-semibold text-cyan-400 bg-cyan-950/30 px-2 py-0.5 rounded border border-cyan-900/30">TCP 5000</span>
                    <div className="flex-1 border-t border-dashed border-cyan-500/30 mx-4 relative">
                      <span className="absolute -top-1.5 left-1/2 -translate-x-1/2 text-[8px] bg-zinc-950 border border-zinc-800 px-1.5 rounded-full text-zinc-400 font-mono">Commands & Telemetry</span>
                    </div>
                    <ArrowRight className="w-3.5 h-3.5 text-cyan-400" />
                  </div>

                  {/* Channel 2 */}
                  <div className="flex items-center justify-between text-xs px-4">
                    <span className="font-mono text-[10px] font-semibold text-blue-400 bg-blue-950/30 px-2 py-0.5 rounded border border-blue-900/30">TCP 5001</span>
                    <div className="flex-1 border-t border-dashed border-blue-500/30 mx-4 relative">
                      <span className="absolute -top-1.5 left-1/2 -translate-x-1/2 text-[8px] bg-zinc-950 border border-zinc-800 px-1.5 rounded-full text-zinc-400 font-mono">Low-latency Video</span>
                    </div>
                    <ArrowRight className="w-3.5 h-3.5 text-blue-400" />
                  </div>

                  {/* Channel 3 */}
                  <div className="flex items-center justify-between text-xs px-4">
                    <span className="font-mono text-[10px] font-semibold text-orange-400 bg-orange-950/30 px-2 py-0.5 rounded border border-orange-900/30">TCP 5002</span>
                    <div className="flex-1 border-t border-dashed border-orange-500/30 mx-4 relative">
                      <span className="absolute -top-1.5 left-1/2 -translate-x-1/2 text-[8px] bg-zinc-950 border border-zinc-800 px-1.5 rounded-full text-zinc-400 font-mono">Direct File Upload/Download</span>
                    </div>
                    <ArrowRight className="w-3.5 h-3.5 text-orange-400" />
                  </div>

                </div>

                {/* Node: Server */}
                <div className="flex items-center justify-between bg-zinc-950 border border-zinc-800 p-3.5 rounded-xl">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-blue-950/50 border border-blue-900 flex items-center justify-center">
                      <Server className="w-4 h-4 text-blue-400" />
                    </div>
                    <div>
                      <span className="block text-xs font-bold text-zinc-200">Nexa Remote Server (Windows)</span>
                      <span className="block text-[10px] text-zinc-500">Inno Setup Background Service</span>
                    </div>
                  </div>
                  <span className="text-[10px] font-mono bg-zinc-900 px-2 py-0.5 rounded border border-zinc-800 text-blue-400">RX Listeners</span>
                </div>

              </div>

            </div>

          </div>

        </div>
      </section>

      {/* 6. AUTOMATIC DISCOVERY */}
      <section id="discovery" className="py-24 relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">

            {/* Explanatory Texts */}
            <div className="lg:col-span-6 space-y-6">
              <span className="text-xs font-bold uppercase tracking-wider text-cyan-400 font-mono">Zero-Config Connectivity</span>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
                Instantly discovered. No tedious manuals.
              </h2>
              <p className="text-zinc-400 text-base leading-relaxed">
                Nexa Remote utilizes a unique fallback system to find your PC on your local Wi-Fi, even when advanced network setups or routing restrictions stand in the way.
              </p>

              {/* Selector Tabs */}
              <div className="space-y-3.5">
                <button
                  onClick={() => setDiscoveryStep('mdns')}
                  className={`w-full text-left p-4 rounded-xl border transition-all duration-200 ${discoveryStep === 'mdns' ? 'bg-cyan-950/20 border-cyan-500/50' : 'bg-zinc-900/20 border-zinc-900 hover:border-zinc-800'}`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="w-6 h-6 rounded bg-cyan-950 flex items-center justify-center text-xs font-bold text-cyan-400 font-mono">1</span>
                    <span className="font-bold text-white text-sm">Primary Discovery: mDNS via Android NsdManager</span>
                  </div>
                  {discoveryStep === 'mdns' && (
                    <p className="text-xs text-zinc-400 mt-2 ml-8.5 pl-0.5 leading-relaxed">
                      Nexa Remote registers an NSD service broadcast (<code className="text-cyan-300">_remote._tcp.local.</code>) when launching. Your Android client queries this instantly, resolving the current IP without any user lookup.
                    </p>
                  )}
                </button>

                <button
                  onClick={() => setDiscoveryStep('probe')}
                  className={`w-full text-left p-4 rounded-xl border transition-all duration-200 ${discoveryStep === 'probe' ? 'bg-cyan-950/20 border-cyan-500/50' : 'bg-zinc-900/20 border-zinc-900 hover:border-zinc-800'}`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="w-6 h-6 rounded bg-cyan-950 flex items-center justify-center text-xs font-bold text-cyan-400 font-mono">2</span>
                    <span className="font-bold text-white text-sm">Fallback: Local Subnet Parallel Probing</span>
                  </div>
                  {discoveryStep === 'probe' && (
                    <p className="text-xs text-zinc-400 mt-2 ml-8.5 pl-0.5 leading-relaxed">
                      If your router blocks multicast traffic (AP Isolation/IGMP Snooping), Nexa scans candidate IPs on the active RFC 1918 subnet dynamically in parallel on port 5000 using Kotlin coroutines.
                    </p>
                  )}
                </button>

                <button
                  onClick={() => setDiscoveryStep('manual')}
                  className={`w-full text-left p-4 rounded-xl border transition-all duration-200 ${discoveryStep === 'manual' ? 'bg-cyan-950/20 border-cyan-500/50' : 'bg-zinc-900/20 border-zinc-900 hover:border-zinc-800'}`}
                >
                  <div className="flex items-center gap-2.5">
                    <span className="w-6 h-6 rounded bg-cyan-950 flex items-center justify-center text-xs font-bold text-cyan-400 font-mono">3</span>
                    <span className="font-bold text-white text-sm">Fail-safe: Manual Direct IP Input</span>
                  </div>
                  {discoveryStep === 'manual' && (
                    <p className="text-xs text-zinc-400 mt-2 ml-8.5 pl-0.5 leading-relaxed">
                      When extreme hardware firewalls are present, bypass automatic routines entirely. Simply copy your server's IPv4 from the tray icon and enter it directly into the mobile client app.
                    </p>
                  )}
                </button>
              </div>

            </div>

            {/* Simulated Live Setup Screen mockup */}
            <div className="lg:col-span-6 bg-zinc-900/25 border border-zinc-900 rounded-3xl p-6 lg:p-8 relative">
              <div className="flex items-center justify-between border-b border-zinc-850 pb-3 mb-6">
                <span className="text-xs font-bold font-mono text-zinc-400">DISCOVERY HANDSHAKE SIMULATION</span>
                <span className="px-2 py-0.5 rounded bg-cyan-500/10 border border-cyan-500/20 text-[10px] text-cyan-400 font-mono">ACTIVE STATE</span>
              </div>

              {discoveryStep === 'mdns' && (
                <div className="space-y-4 font-mono text-xs">
                  <div className="text-zinc-500">// Client looking for mDNS services...</div>
                  <div className="text-cyan-400 flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
                    [Client] Listening for service registration: _remote._tcp.local.
                  </div>
                  <div className="text-emerald-400">[Server] Found active broadcast! PC IP resolved to 192.168.1.104:5000</div>
                  <div className="p-3 bg-zinc-950 rounded-lg border border-zinc-850 flex items-center justify-between">
                    <div>
                      <span className="block font-bold text-zinc-200">DESKTOP-8K9A2LM</span>
                      <span className="text-[10px] text-zinc-500">IP: 192.168.1.104</span>
                    </div>
                    <button className="px-3 py-1 bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-400 rounded-lg border border-cyan-500/30 text-[11px] font-bold">Connect</button>
                  </div>
                </div>
              )}

              {discoveryStep === 'probe' && (
                <div className="space-y-3 font-mono text-xs">
                  <div className="text-zinc-500">// Multicast blocked. Initiating parallel subnet sweep...</div>
                  <div className="text-yellow-400">Scanning subnet: 192.168.1.0/24...</div>
                  <div className="grid grid-cols-2 gap-2 text-[10px] text-zinc-600">
                    <div>Probe 192.168.1.10 ... Connection Timeout</div>
                    <div>Probe 192.168.1.25 ... Connection Timeout</div>
                    <div className="text-emerald-500/80">Probe 192.168.1.104 ... Server Identity Matches!</div>
                    <div>Probe 192.168.1.120 ... Connection Timeout</div>
                  </div>
                  <div className="p-3 bg-zinc-950 rounded-lg border border-zinc-850 flex items-center justify-between">
                    <div>
                      <span className="block font-bold text-zinc-200">NexaServer_Local</span>
                      <span className="text-[10px] text-zinc-500">IP: 192.168.1.104</span>
                    </div>
                    <button className="px-3 py-1 bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-400 rounded-lg border border-cyan-500/30 text-[11px] font-bold">Connect</button>
                  </div>
                </div>
              )}

              {discoveryStep === 'manual' && (
                <div className="space-y-4 text-xs font-mono">
                  <div className="text-zinc-500">// Manual socket entry fallback interface</div>
                  <div className="space-y-2">
                    <label className="text-zinc-400 block text-[11px]">Enter Local Server IPv4 Address</label>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        readOnly
                        value="192.168.1.104"
                        className="bg-zinc-950 border border-zinc-800 rounded px-3 py-1.5 text-zinc-200 font-mono flex-1 focus:outline-none"
                      />
                      <button className="px-4 py-1.5 bg-gradient-to-r from-cyan-500 to-blue-600 rounded text-white font-bold text-xs">Link PC</button>
                    </div>
                  </div>
                  <div className="text-emerald-400 flex items-center gap-1.5 text-[11px]">
                    <Check className="w-3.5 h-3.5" /> Core handshake verified on Port 5000 successfully!
                  </div>
                </div>
              )}

            </div>

          </div>

        </div>
      </section>

      {/* 7. SECURITY DEDICATED SECTION */}
      <section id="security" className="py-24 border-t border-zinc-900 bg-zinc-950 relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">

            {/* Visual Security lock representation */}
            <div className="lg:col-span-5 flex justify-center order-last lg:order-first">
              <div className="relative w-full max-w-[340px] aspect-square bg-gradient-to-tr from-cyan-950/20 to-zinc-900/30 border border-zinc-800 rounded-3xl p-8 flex flex-col justify-between overflow-hidden shadow-2xl">
                <div className="absolute top-[-20%] right-[-20%] w-[150px] h-[150px] bg-cyan-500/5 rounded-full blur-2xl pointer-events-none" />

                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono text-cyan-400 tracking-wider">SECURITY AUDIT PERSISTENCE</span>
                  <Shield className="w-5 h-5 text-cyan-400" />
                </div>

                <div className="my-8 flex justify-center relative">
                  {/* Glowing padlock or dynamic elements */}
                  <div className="w-20 h-20 rounded-2xl bg-zinc-900 border border-zinc-800 flex items-center justify-center relative z-10 shadow-xl">
                    <Key className="w-10 h-10 text-cyan-400" />
                  </div>
                  <div className="absolute inset-0 bg-cyan-500/10 rounded-full blur-3xl scale-75 animate-pulse-slow" />
                </div>

                <div className="space-y-2 text-[10px] font-mono">
                  <div className="flex justify-between text-zinc-500">
                    <span>CIPHER METHOD</span>
                    <span className="text-zinc-200">AES-256-GCM</span>
                  </div>
                  <div className="flex justify-between text-zinc-500">
                    <span>KEYSTORE STORAGE</span>
                    <span className="text-zinc-200">AndroidKeyStore</span>
                  </div>
                  <div className="flex justify-between text-zinc-500">
                    <span>COMMAND SANITY</span>
                    <span className="text-emerald-400 font-bold">STRICT ALLOWLIST</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Security content */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-950/40 border border-emerald-900/50 text-emerald-400 font-medium text-xs">
                <Shield className="w-3.5 h-3.5" />
                No False Claims. Just Transparent Local Cryptography.
              </div>

              <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
                Built secure by design. <br className="hidden sm:inline" />
                Your data stays in your home.
              </h2>

              <p className="text-zinc-400 text-base leading-relaxed">
                Nexa Remote does not make broad "military-grade" promises. Instead, we secure your device connection with straightforward local-first controls:
              </p>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">

                <div className="space-y-1.5">
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    Strictly Local Sockets
                  </h4>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    Commands and stream bytes are written only to local interfaces. No remote databases, cloud servers, or relays are involved in routing data.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    Temporary 6-Digit PIN
                  </h4>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    New connection requests display a random PIN on the host monitor. To prevent accidental connections, you must authorize access locally on first-run.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    Keystore Authentication
                  </h4>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    Authorization tokens are generated cryptographically and stored inside the Android hardware-backed KeyStore container using authenticated AES-GCM.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <h4 className="text-sm font-bold text-white flex items-center gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    Launcher Shell Safeguards
                  </h4>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    Program commands are matched against a strict runtime allowlist. Arbitrary command injection or script execution is fundamentally blocked at the server level.
                  </p>
                </div>

              </div>

            </div>

          </div>

        </div>
      </section>

      {/* 8. PRODUCT EXPERIENCE / LUSTRE SHOWCASE */}
      <section id="showcase" className="py-24 border-t border-zinc-900 bg-zinc-900/10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="text-center max-w-3xl mx-auto mb-16">
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
              Interactive Live Experience
            </h2>
            <p className="text-zinc-400 text-lg">
              Interact with the tabs below to explore the exact Android client screens and PC telemetry controls in real-time.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">

            {/* Sidebar selection tabs */}
            <div className="lg:col-span-4 flex flex-col gap-3">
              <button
                onClick={() => setActiveExperienceTab('trackpad')}
                className={`p-4 text-left rounded-2xl border transition-all duration-200 flex items-start gap-3.5 ${activeExperienceTab === 'trackpad' ? 'bg-zinc-900 border-zinc-800 text-white shadow-xl shadow-cyan-500/5' : 'bg-transparent border-transparent text-zinc-400 hover:text-zinc-200'}`}
              >
                <MousePointer className={`w-5 h-5 mt-0.5 ${activeExperienceTab === 'trackpad' ? 'text-cyan-400' : 'text-zinc-500'}`} />
                <div>
                  <span className="block font-bold text-sm">Remote Touchpad Control</span>
                  <span className="block text-xs text-zinc-500 mt-1">Tap, scroll, drag, and click with precision.</span>
                </div>
              </button>

              <button
                onClick={() => setActiveExperienceTab('streaming')}
                className={`p-4 text-left rounded-2xl border transition-all duration-200 flex items-start gap-3.5 ${activeExperienceTab === 'streaming' ? 'bg-zinc-900 border-zinc-800 text-white shadow-xl shadow-cyan-500/5' : 'bg-transparent border-transparent text-zinc-400 hover:text-zinc-200'}`}
              >
                <Monitor className={`w-5 h-5 mt-0.5 ${activeExperienceTab === 'streaming' ? 'text-cyan-400' : 'text-zinc-500'}`} />
                <div>
                  <span className="block font-bold text-sm">Real-time Desktop Stream</span>
                  <span className="block text-xs text-zinc-500 mt-1">Low latency mirror with active scaling.</span>
                </div>
              </button>

              <button
                onClick={() => setActiveExperienceTab('telemetry')}
                className={`p-4 text-left rounded-2xl border transition-all duration-200 flex items-start gap-3.5 ${activeExperienceTab === 'telemetry' ? 'bg-zinc-900 border-zinc-800 text-white shadow-xl shadow-cyan-500/5' : 'bg-transparent border-transparent text-zinc-400 hover:text-zinc-200'}`}
              >
                <Activity className={`w-5 h-5 mt-0.5 ${activeExperienceTab === 'telemetry' ? 'text-cyan-400' : 'text-zinc-500'}`} />
                <div>
                  <span className="block font-bold text-sm">PC Hardware Telemetry</span>
                  <span className="block text-xs text-zinc-500 mt-1">Monitor live diagnostics of CPU and RAM load.</span>
                </div>
              </button>

              <button
                onClick={() => setActiveExperienceTab('launcher')}
                className={`p-4 text-left rounded-2xl border transition-all duration-200 flex items-start gap-3.5 ${activeExperienceTab === 'launcher' ? 'bg-zinc-900 border-zinc-800 text-white shadow-xl shadow-cyan-500/5' : 'bg-transparent border-transparent text-zinc-400 hover:text-zinc-200'}`}
              >
                <AppWindow className={`w-5 h-5 mt-0.5 ${activeExperienceTab === 'launcher' ? 'text-cyan-400' : 'text-zinc-500'}`} />
                <div>
                  <span className="block font-bold text-sm">Instant App Launcher</span>
                  <span className="block text-xs text-zinc-500 mt-1">Boot pre-approved Windows applications.</span>
                </div>
              </button>
            </div>

            {/* Live Interactive Canvas Screen mockup container */}
            <div className="lg:col-span-8 flex justify-center">

              <div className="w-full max-w-[500px] bg-zinc-950 border border-zinc-900 rounded-[40px] shadow-2xl p-6 relative overflow-hidden flex flex-col justify-between items-center min-h-[460px]">

                {/* Simulated Glass Notch/Dynamic-island style header */}
                <div className="w-24 h-4 bg-zinc-900 rounded-full flex items-center justify-center mb-6">
                  <span className="w-1.5 h-1.5 rounded-full bg-zinc-950" />
                </div>

                {/* TAB 1: TRACKPAD */}
                {activeExperienceTab === 'trackpad' && (
                  <div className="w-full flex-1 flex flex-col justify-between items-center">
                    <div className="text-center mb-4">
                      <span className="text-xs text-cyan-400 font-mono block">CLICK THE BOUNDS TO INTERACT</span>
                      <h4 className="text-sm font-semibold text-zinc-300">Virtual Touchpad Surface</h4>
                    </div>

                    {/* The Trackpad Pad */}
                    <div className="w-full max-w-[340px] h-52 bg-zinc-900/60 rounded-3xl border border-zinc-800 flex flex-col items-center justify-center relative cursor-crosshair group hover:border-cyan-500/30 transition-colors">
                      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                        <div className="w-12 h-12 rounded-full border border-dashed border-cyan-500/20 group-hover:scale-125 transition-transform duration-500" />
                      </div>
                      <MousePointer className="w-8 h-8 text-cyan-400 mb-2 animate-bounce" />
                      <span className="text-[10px] text-zinc-500 select-none">Slide to move mouse cursor</span>
                    </div>

                    {/* Left / Right click buttons */}
                    <div className="w-full max-w-[340px] grid grid-cols-2 gap-4 mt-6">
                      <button
                        onClick={() => triggerClick('left')}
                        className={`py-3.5 text-xs font-bold font-mono rounded-xl border transition-all duration-150 ${trackpadClicks.left ? 'bg-cyan-500/20 border-cyan-500 text-white scale-95' : 'bg-zinc-900 border-zinc-800 text-zinc-400'}`}
                      >
                        LEFT CLICK
                      </button>
                      <button
                        onClick={() => triggerClick('right')}
                        className={`py-3.5 text-xs font-bold font-mono rounded-xl border transition-all duration-150 ${trackpadClicks.right ? 'bg-cyan-500/20 border-cyan-500 text-white scale-95' : 'bg-zinc-900 border-zinc-800 text-zinc-400'}`}
                      >
                        RIGHT CLICK
                      </button>
                    </div>
                  </div>
                )}

                {/* TAB 2: STREAMING */}
                {activeExperienceTab === 'streaming' && (
                  <div className="w-full flex-1 flex flex-col justify-between">
                    <div className="flex items-center justify-between px-2 mb-4">
                      <div className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                        <span className="text-[10px] font-mono font-bold text-zinc-400">LIVE SCREEN &middot; 1080P @ 60FPS</span>
                      </div>
                      <span className="text-[10px] text-zinc-500 font-mono">Scale: {zoomLevel}%</span>
                    </div>

                    {/* Streaming mockup */}
                    <div className="w-full h-48 bg-zinc-900 rounded-2xl border border-zinc-800 relative overflow-hidden flex items-center justify-center">
                      <div
                        className="w-[120%] h-[120%] bg-zinc-950 border border-zinc-850 rounded-lg p-2.5 flex flex-col justify-between shadow-inner transition-transform duration-300"
                        style={{ transform: `scale(${zoomLevel / 100})` }}
                      >
                        <div className="flex items-center justify-between border-b border-zinc-900 pb-1">
                          <span className="text-[8px] font-mono text-zinc-500">Workspace 1</span>
                          <span className="text-[8px] font-mono text-zinc-600">Localhost</span>
                        </div>
                        {/* Windows Screen Visual */}
                        <div className="flex-1 flex flex-col justify-center items-center gap-2 py-2">
                          <div className="w-3/4 h-2 bg-zinc-900 rounded" />
                          <div className="w-1/2 h-2 bg-zinc-900 rounded" />
                          <div className="grid grid-cols-4 gap-1 w-3/4 mt-2">
                            <div className="h-6 bg-zinc-900 rounded" />
                            <div className="h-6 bg-cyan-950/40 border border-cyan-900/30 rounded" />
                            <div className="h-6 bg-zinc-900 rounded" />
                            <div className="h-6 bg-zinc-900 rounded" />
                          </div>
                        </div>
                        <div className="h-3 bg-zinc-900/80 rounded flex items-center px-1 text-[7px] text-zinc-500">
                          Windows Taskbar
                        </div>
                      </div>
                    </div>

                    {/* Scale Controls */}
                    <div className="flex items-center justify-between mt-5 px-1">
                      <div className="flex gap-2">
                        <button
                          onClick={() => setZoomLevel(Math.max(50, zoomLevel - 25))}
                          className="px-3 py-1.5 bg-zinc-900 hover:bg-zinc-850 rounded border border-zinc-800 text-[10px] font-bold text-zinc-300"
                        >
                          ZOOM -
                        </button>
                        <button
                          onClick={() => setZoomLevel(Math.min(150, zoomLevel + 25))}
                          className="px-3 py-1.5 bg-zinc-900 hover:bg-zinc-850 rounded border border-zinc-800 text-[10px] font-bold text-zinc-300"
                        >
                          ZOOM +
                        </button>
                      </div>

                      {/* Stream Quality Selector */}
                      <div className="flex bg-zinc-900 rounded-lg p-1 border border-zinc-850">
                        {['LOW', 'MED', 'HIGH'].map(q => (
                          <button
                            key={q}
                            onClick={() => setStreamQuality(q)}
                            className={`px-2.5 py-1 text-[8px] font-bold rounded-md transition-all ${streamQuality === q ? 'bg-cyan-500 text-white' : 'text-zinc-500 hover:text-zinc-300'}`}
                          >
                            {q}
                          </button>
                        ))}
                      </div>
                    </div>

                  </div>
                )}

                {/* TAB 3: TELEMETRY */}
                {activeExperienceTab === 'telemetry' && (
                  <div className="w-full flex-1 flex flex-col justify-between">
                    <div className="text-center mb-4">
                      <span className="text-xs text-cyan-400 font-mono block">LIVE HOST SYSTEM STATISTICS</span>
                      <h4 className="text-sm font-semibold text-zinc-300">PC Performance Monitor</h4>
                    </div>

                    {/* Dials & Progress Bars */}
                    <div className="grid grid-cols-2 gap-4 my-2">
                      {/* CPU Circle dial */}
                      <div className="bg-zinc-900/60 rounded-2xl border border-zinc-800 p-4 flex flex-col items-center justify-center">
                        <div className="relative w-20 h-20 flex items-center justify-center mb-2">
                          {/* Radial Progress Arc */}
                          <svg className="absolute inset-0 w-full h-full -rotate-90">
                            <circle cx="40" cy="40" r="32" className="stroke-zinc-800" strokeWidth="6" fill="transparent" />
                            <circle cx="40" cy="40" r="32" className="stroke-cyan-400 transition-all duration-500" strokeWidth="6" fill="transparent" strokeDasharray="201" strokeDashoffset={201 - (201 * cpuUsage) / 100} />
                          </svg>
                          <span className="text-sm font-mono font-bold text-white">{cpuUsage}%</span>
                        </div>
                        <span className="text-[10px] text-zinc-500 font-bold uppercase">CPU Load</span>
                      </div>

                      {/* RAM Circle dial */}
                      <div className="bg-zinc-900/60 rounded-2xl border border-zinc-800 p-4 flex flex-col items-center justify-center">
                        <div className="relative w-20 h-20 flex items-center justify-center mb-2">
                          <svg className="absolute inset-0 w-full h-full -rotate-90">
                            <circle cx="40" cy="40" r="32" className="stroke-zinc-800" strokeWidth="6" fill="transparent" />
                            <circle cx="40" cy="40" r="32" className="stroke-blue-500 transition-all duration-500" strokeWidth="6" fill="transparent" strokeDasharray="201" strokeDashoffset={201 - (201 * ramUsage) / 100} />
                          </svg>
                          <span className="text-sm font-mono font-bold text-white">{ramUsage}%</span>
                        </div>
                        <span className="text-[10px] text-zinc-500 font-bold uppercase">RAM Usage</span>
                      </div>
                    </div>

                    {/* Storage & Uptime lists */}
                    <div className="space-y-2 mt-4 font-mono text-[10px] bg-zinc-900/40 p-3 rounded-xl border border-zinc-850">
                      <div className="flex justify-between items-center">
                        <span className="text-zinc-500">HOST UPTIME</span>
                        <span className="text-zinc-200">{uptime}</span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="text-zinc-500">DISK SPACE (C:)</span>
                        <span className="text-zinc-200">241.5 GB FREE / 512 GB</span>
                      </div>
                    </div>

                  </div>
                )}

                {/* TAB 4: LAUNCHER */}
                {activeExperienceTab === 'launcher' && (
                  <div className="w-full flex-1 flex flex-col justify-between">
                    <div className="text-center mb-4">
                      <span className="text-xs text-cyan-400 font-mono block">CLICK AN ICON TO LAUNCH</span>
                      <h4 className="text-sm font-semibold text-zinc-300">Fast Executable Allowlist</h4>
                    </div>

                    {/* App Grid */}
                    <div className="grid grid-cols-3 gap-3 my-2">
                      <button
                        onClick={() => handleLaunchApp('Google Chrome')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <Globe className="w-5 h-5 text-cyan-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">Chrome</span>
                      </button>

                      <button
                        onClick={() => handleLaunchApp('VS Code')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <Terminal className="w-5 h-5 text-blue-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">VS Code</span>
                      </button>

                      <button
                        onClick={() => handleLaunchApp('Task Manager')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <Activity className="w-5 h-5 text-emerald-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">Taskmgr</span>
                      </button>

                      <button
                        onClick={() => handleLaunchApp('File Explorer')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <HardDrive className="w-5 h-5 text-yellow-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">Explorer</span>
                      </button>

                      <button
                        onClick={() => handleLaunchApp('Spotify')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <Zap className="w-5 h-5 text-purple-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">Spotify</span>
                      </button>

                      <button
                        onClick={() => handleLaunchApp('Notepad')}
                        className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 hover:border-cyan-500/40 hover:bg-zinc-850 text-center transition-all flex flex-col items-center"
                      >
                        <FileText className="w-5 h-5 text-orange-400 mb-1" />
                        <span className="text-[9px] text-zinc-300 font-bold">Notepad</span>
                      </button>
                    </div>

                    {/* Launch feedback state */}
                    <div className="h-10 flex items-center justify-center mt-4">
                      {launchedApp ? (
                        <div className="px-3.5 py-1.5 bg-cyan-950/40 border border-cyan-800 text-cyan-400 rounded-lg text-[10px] font-mono animate-pulse flex items-center gap-2">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Host Server running: Launching {launchedApp}...
                        </div>
                      ) : (
                        <span className="text-[9px] text-zinc-600 font-mono">Select an application to trigger mock PC launch event</span>
                      )}
                    </div>

                  </div>
                )}

                {/* Bottom glass reflection indicator */}
                <div className="w-20 h-1 bg-zinc-900 rounded-full mt-6" />

              </div>

            </div>

          </div>

        </div>
      </section>

      {/* 9. INSTALLATION GUIDE */}
      <section id="install" className="py-24 border-t border-zinc-900 bg-zinc-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="text-center max-w-3xl mx-auto mb-16">
            <span className="text-xs font-bold uppercase tracking-wider text-cyan-400 font-mono">STEP-BY-STEP DEPLOYMENT</span>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
              Get Started in Under 2 Minutes
            </h2>
            <p className="text-zinc-400 text-base">
              Nexa Remote runs completely independent of accounts or internet setups. Follow these steps to pair your client with your desktop.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">

            {/* Windows Column */}
            <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl p-6 lg:p-8 relative">
              <div className="absolute top-4 right-4 p-2 rounded-xl bg-blue-950/50 border border-blue-900/30">
                <Monitor className="w-5 h-5 text-blue-400" />
              </div>

              <h3 className="text-lg font-bold text-white mb-1">1. Windows Host Setup</h3>
              <p className="text-xs text-zinc-500 mb-6">Install and launch the local background service</p>

              <div className="space-y-6">
                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">1</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Download Installer</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      Download <code className="text-cyan-400">Nexa Remote Setup.exe</code> from the V1.0.1 download panel below or release page.
                    </p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">2</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Approve Firewall Exceptions</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      When prompted during setup, accept Windows Defender Firewall rules to unlock local connections on ports <code className="text-cyan-400">5000, 5001, 5002</code>.
                    </p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">3</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Launches Silently</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      The service registers to boot automatically on startup and rests in your system tray without interrupting other work.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Android Column */}
            <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl p-6 lg:p-8 relative">
              <div className="absolute top-4 right-4 p-2 rounded-xl bg-cyan-950/50 border border-cyan-900/30">
                <Smartphone className="w-5 h-5 text-cyan-400" />
              </div>

              <h3 className="text-lg font-bold text-white mb-1">2. Android Client Setup</h3>
              <p className="text-xs text-zinc-500 mb-6">Install and pair with your Windows host</p>

              <div className="space-y-6">
                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">1</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Install APK Package</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      Download <code className="text-cyan-400">Nexa Remote 1.0.1.apk</code> and tap file in file manager to install on your phone or tablet.
                    </p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">2</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Connect to Local Wi-Fi</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      Ensure your Android device has Wi-Fi enabled and is connected to the exact same router subnet as the Windows PC.
                    </p>
                  </div>
                </div>

                <div className="flex gap-4">
                  <span className="w-7 h-7 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-bold text-zinc-300 flex items-center justify-center shrink-0">3</span>
                  <div>
                    <h5 className="text-sm font-bold text-zinc-200">Pair via 6-Digit PIN</h5>
                    <p className="text-xs text-zinc-400 mt-1">
                      Select your PC in the discovered PC list. Type the 6-digit verification PIN shown on your PC screen to complete secure pairing.
                    </p>
                  </div>
                </div>
              </div>
            </div>

          </div>

        </div>
      </section>

      {/* 10. DOWNLOAD CALL TO ACTION */}
      <section id="download" className="py-24 relative bg-zinc-900/30 border-t border-zinc-900">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-cyan-500/5 rounded-full blur-[140px] pointer-events-none" />

        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative z-10">

          <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
            Ready to control your environment?
          </h2>
          <p className="text-zinc-400 text-lg max-w-xl mx-auto mb-10 leading-relaxed">
            Get the latest stable V1.0.1 binaries directly and secure your own private PC remote control dashboard instantly.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">

            <a
              href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.1.0.1.apk"
              className="flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold rounded-xl shadow-xl shadow-cyan-500/10 hover:shadow-cyan-400/20 transition-all duration-200 w-full sm:w-auto justify-center"
            >
              <Download className="w-5 h-5" />
              Download Android APK V1.0.1
            </a>

            <a
              href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.Setup.exe"
              className="flex items-center gap-3 px-8 py-4 bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 hover:border-zinc-700 text-zinc-200 hover:text-white font-bold rounded-xl transition-all duration-200 w-full sm:w-auto justify-center"
            >
              <Download className="w-5 h-5" />
              Download Windows Setup V1.0.1
            </a>

          </div>

          <div className="mt-8 text-sm text-zinc-500">
            MD5/SHA256 checksums available in official release directory.{' '}
            <a href="https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1" target="_blank" rel="noopener noreferrer" className="text-cyan-500 hover:underline">Verify Signatures &rarr;</a>
          </div>

        </div>
      </section>

      {/* 11. SYSTEM REQUIREMENTS */}
      <section className="py-24 border-t border-zinc-900 bg-zinc-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="max-w-3xl mx-auto text-center mb-12">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">System Specifications & Requirements</h2>
            <p className="text-xs text-zinc-500 mt-2">Nexa Remote runs beautifully on lightweight specs, keeping memory footprints small.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl mx-auto">

            {/* Windows requirements */}
            <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 font-sans">
              <h4 className="font-bold text-sm text-zinc-200 mb-4 pb-2 border-b border-zinc-850 flex items-center gap-2">
                <Monitor className="w-4 h-4 text-blue-400" /> Windows Server
              </h4>
              <ul className="space-y-2.5 text-xs text-zinc-400">
                <li className="flex justify-between"><span className="text-zinc-500">Operating System</span> <span className="text-zinc-200">Windows 10 / Windows 11 (64-bit)</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Memory</span> <span className="text-zinc-200">15MB RAM idle footprint</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Framework</span> <span className="text-zinc-200">Pre-compiled Native C++ / Win32 API</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Network Ports</span> <span className="text-zinc-200">Inbound TCP 5000, 5001, 5002 open</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Startup Type</span> <span className="text-zinc-200">Optional automatic tray startup</span></li>
              </ul>
            </div>

            {/* Android requirements */}
            <div className="bg-zinc-900/30 border border-zinc-900 rounded-2xl p-6 font-sans">
              <h4 className="font-bold text-sm text-zinc-200 mb-4 pb-2 border-b border-zinc-850 flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-cyan-400" /> Android Client
              </h4>
              <ul className="space-y-2.5 text-xs text-zinc-400">
                <li className="flex justify-between"><span className="text-zinc-500">API Compatibility</span> <span className="text-zinc-200">Android 7.0 (API 24) or higher</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Storage</span> <span className="text-zinc-200">Approx. 12MB install weight</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Client UI engine</span> <span className="text-zinc-200">Modern Jetpack Compose architecture</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Authorization Storage</span> <span className="text-zinc-200">Encrypted AndroidKeyStore container</span></li>
                <li className="flex justify-between"><span className="text-zinc-500">Wi-Fi / LAN</span> <span className="text-zinc-200">mDNS service discovery permission</span></li>
              </ul>
            </div>

          </div>

        </div>
      </section>

      {/* 12. FAQ / TROUBLESHOOTING */}
      <section id="faq" className="py-24 border-t border-zinc-900 bg-zinc-900/10">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="text-center mb-16">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-zinc-900 border border-zinc-800 text-xs text-zinc-400 font-medium mb-3">
              <HelpCircle className="w-3.5 h-3.5 text-cyan-400" />
              Frequently Answered Questions
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-white mb-4 tracking-tight">
              Troubleshooting & Support
            </h2>
            <p className="text-zinc-400 text-base">
              Got questions? Here is how to diagnose common local connectivity issues.
            </p>
          </div>

          <div className="space-y-4">
            {faqs.map((faq, idx) => (
              <div
                key={idx}
                className="bg-zinc-950/80 border border-zinc-900 rounded-2xl overflow-hidden transition-all duration-300"
              >
                <button
                  onClick={() => setOpenFaqIndex(openFaqIndex === idx ? null : idx)}
                  className="w-full flex justify-between items-center p-6 text-left hover:bg-zinc-900/40 focus:outline-none"
                >
                  <span className="font-bold text-white text-base leading-relaxed pr-4">{faq.q}</span>
                  <ChevronDown className={`w-5 h-5 text-zinc-500 shrink-0 transition-transform duration-300 ${openFaqIndex === idx ? 'rotate-180 text-cyan-400' : ''}`} />
                </button>

                <div
                  className={`transition-all duration-300 ease-in-out overflow-hidden ${openFaqIndex === idx ? 'max-h-96 border-t border-zinc-900/60' : 'max-h-0'}`}
                >
                  <p className="p-6 text-sm text-zinc-400 leading-relaxed bg-zinc-950">
                    {faq.a}
                  </p>
                </div>
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* 13. GITHUB / COMMUNITY */}
      <section className="py-24 border-t border-zinc-900 bg-zinc-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="bg-gradient-to-tr from-cyan-950/20 via-zinc-900/10 to-zinc-950 border border-zinc-900 rounded-3xl p-8 lg:p-12 shadow-2xl flex flex-col lg:flex-row justify-between items-center gap-8 relative overflow-hidden">
            <div className="absolute top-0 right-0 bg-cyan-500/5 w-60 h-60 rounded-full blur-[100px] pointer-events-none" />

            <div className="space-y-4 text-center lg:text-left max-w-2xl">
              <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">Fully Transparent. Open for Audits.</h2>
              <p className="text-zinc-400 text-sm leading-relaxed">
                Nexa Remote is completely open-source under the permissive MIT License. Browse, audit, or branch the code directly on GitHub. Join our network of contributors helping shape clean, private system tools.
              </p>

              <div className="flex flex-wrap gap-4 justify-center lg:justify-start pt-2 text-[11px] font-mono text-zinc-500">
                <a href="https://github.com/Srinivasrao422/Nexa-Remote" className="hover:underline hover:text-zinc-300">View Repository</a>
                <span>&middot;</span>
                <a href="https://github.com/Srinivasrao422/Nexa-Remote/blob/main/SECURITY.md" className="hover:underline hover:text-zinc-300">Security Policy</a>
                <span>&middot;</span>
                <a href="https://github.com/Srinivasrao422/Nexa-Remote/blob/main/CONTRIBUTING.md" className="hover:underline hover:text-zinc-300">Contributing Guidelines</a>
                <span>&middot;</span>
                <a href="https://github.com/Srinivasrao422/Nexa-Remote/blob/main/LICENSE" className="hover:underline hover:text-zinc-300">MIT License</a>
              </div>
            </div>

            <div className="shrink-0">
              <a
                href="https://github.com/Srinivasrao422/Nexa-Remote"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-2 px-6 py-3.5 bg-zinc-900 border border-zinc-800 hover:border-zinc-700 text-zinc-200 hover:text-white rounded-xl font-semibold transition-all duration-200 shadow-xl"
              >
                <Github className="w-5 h-5" />
                <span>Visit Nexa on GitHub</span>
              </a>
            </div>

          </div>

        </div>
      </section>

      {/* 14. FOOTER */}
      <footer className="bg-zinc-950 border-t border-zinc-900 py-12 relative z-10 text-xs text-zinc-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-12">

            <div className="space-y-4">
              <span className="font-extrabold text-sm text-white">Nexa Remote</span>
              <p className="text-zinc-500 leading-relaxed">
                A safe, secure, local-network alternative for desktop management. Control inputs, stream frames, launch apps, and audit code securely.
              </p>
            </div>

            <div>
              <span className="font-bold text-zinc-300 block mb-4">Releases</span>
              <ul className="space-y-2.5">
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/releases/tag/v1.0.1" className="hover:underline hover:text-zinc-300">Version V1.0.1 Release</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.1.0.1.apk" className="hover:underline hover:text-zinc-300">Android APK package</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/releases/download/v1.0.1/Nexa.Remote.Setup.exe" className="hover:underline hover:text-zinc-300">Windows Installer exe</a></li>
              </ul>
            </div>

            <div>
              <span className="font-bold text-zinc-300 block mb-4">Documentation</span>
              <ul className="space-y-2.5">
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote#features" className="hover:underline hover:text-zinc-300">Features Manual</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote#troubleshooting" className="hover:underline hover:text-zinc-300">Connectivity Check</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote#pairing-procedure" className="hover:underline hover:text-zinc-300">Pairing Procedure</a></li>
              </ul>
            </div>

            <div>
              <span className="font-bold text-zinc-300 block mb-4">Community</span>
              <ul className="space-y-2.5">
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/issues" className="hover:underline hover:text-zinc-300">Report an Issue</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/pulls" className="hover:underline hover:text-zinc-300">Submit Pull Request</a></li>
                <li><a href="https://github.com/Srinivasrao422/Nexa-Remote/blob/main/LICENSE" className="hover:underline hover:text-zinc-300">License Terms (MIT)</a></li>
              </ul>
            </div>

          </div>

          <div className="border-t border-zinc-900 pt-8 flex flex-col sm:flex-row justify-between items-center gap-4">
            <span>&copy; {new Date().getFullYear()} Nexa Remote. Licensed under MIT.</span>
            <span>Made with precision for local-first desktop control.</span>
          </div>

        </div>
      </footer>

    </div>
  );
}

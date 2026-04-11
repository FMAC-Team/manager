import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'navbar.dart'; 

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const NavBarApp());
}

class NavBarApp extends StatefulWidget {
  const NavBarApp({super.key});
  @override
  State<NavBarApp> createState() => _NavBarAppState();
}

class _NavBarAppState extends State<NavBarApp> {
  int _selectedIndex = 0;
  ColorScheme? _dynamicScheme;
  static const _channel = MethodChannel('nekosu.aqnya/navbar');

  @override
  void initState() {
    super.initState();
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'setIndex':
          if (mounted) setState(() => _selectedIndex = call.arguments as int);
        case 'setColors':
          final m = Map<String, int>.from(call.arguments as Map);
          if (mounted) setState(() => _dynamicScheme = _buildScheme(m));
      }
    });
    _channel.invokeMethod('requestColors');
  }

  ColorScheme _buildScheme(Map<String, int> m) {
    Color c(String k) => Color(m[k]!);
    final base = ColorScheme.fromSeed(
      seedColor: c('secondaryContainer'),
      brightness: WidgetsBinding.instance.platformDispatcher.platformBrightness,
    );
    return base.copyWith(
      surfaceContainer:     c('surfaceContainer'),
      secondaryContainer:   c('secondaryContainer'),
      onSecondaryContainer: c('onSecondaryContainer'),
      onSurfaceVariant:     c('onSurfaceVariant'),
      surfaceTint:          c('surfaceTint'),
    );
  }

  void _onTabSelected(int i) {
    setState(() => _selectedIndex = i);
    _channel.invokeMethod('onTabSelected', i);
  }

  @override
  Widget build(BuildContext context) {
if (_dynamicScheme == null) {
      return const ColoredBox(color: Colors.transparent);
    }
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: Colors.transparent,
        colorScheme: _dynamicScheme!
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: Colors.transparent,
        colorScheme: _dynamicScheme!,
      ),
      themeMode: ThemeMode.system,
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: TweenAnimationBuilder<double>(
          tween: Tween<double>(begin: 0.0, end: 1.0),
          duration: const Duration(milliseconds: 600),
          curve: Curves.easeOutCubic, 
          builder: (context, value, child) {
            return Transform.translate(
              offset: Offset(0, 80 * (1 - value)),
              child: Opacity(
                opacity: value.clamp(0.0, 1.0),
                child: child,
              ),
            );
          },
          child: ModernCapsuleNavBar(
            selectedIndex: _selectedIndex,
            onTabSelected: _onTabSelected,
            tabs: const [
              NavBarTab(label: '主页', icon: Icon(Icons.home_outlined),    activeIcon: Icon(Icons.home_rounded)),
              NavBarTab(label: '应用', icon: Icon(Icons.apps_outlined),    activeIcon: Icon(Icons.apps_rounded)),
              NavBarTab(label: '设置', icon: Icon(Icons.settings_outlined), activeIcon: Icon(Icons.settings_rounded)),
            ],
          ),
        ),
      ),

    );
  }
}
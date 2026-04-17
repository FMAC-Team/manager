import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'navbar.dart';
import 'settings.dart';

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
  bool _navBarVisible = true;
  bool _showRules = false;
  ColorScheme? _dynamicScheme;
  static const _channel = MethodChannel('nekosu.aqnya/navbar');

  @override
  void initState() {
    super.initState();
    _initData();
    
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'setIndex':
          if (mounted) {
            setState(() {
              int newIndex = call.arguments as int;
              int maxIndex = _showRules ? 3 : 2;
              _selectedIndex = newIndex > maxIndex ? maxIndex : newIndex;
            });
          }
        case 'setColors':
          final m = Map<String, int>.from(call.arguments as Map);
          if (mounted) setState(() => _dynamicScheme = _buildScheme(m));
        case 'setNavBarVisible':
          if (mounted) setState(() => _navBarVisible = call.arguments as bool);
      }
    });
    _channel.invokeMethod('requestColors');
  }
  
  Future<void> _initData() async {
    final showRules = await DebugConfig.getShowRules();
    
    if (mounted) {
      setState(() {
        _showRules = showRules;
        int maxIndex = _showRules ? 3 : 2;
        if (_selectedIndex > maxIndex) {
          _selectedIndex = maxIndex;
          _channel.invokeMethod('onTabSelected', _selectedIndex);
        }
      });
    }
  }

  ColorScheme _buildScheme(Map<String, int> m) {
    Color c(String k) => Color(m[k]!);
    final base = ColorScheme.fromSeed(
      seedColor: c('secondaryContainer'),
      brightness: WidgetsBinding.instance.platformDispatcher.platformBrightness,
    );
    return base.copyWith(
      surfaceContainer: c('surfaceContainer'),
      secondaryContainer: c('secondaryContainer'),
      onSecondaryContainer: c('onSecondaryContainer'),
      onSurfaceVariant: c('onSurfaceVariant'),
      surfaceTint: c('surfaceTint'),
    );
  }

  void _onTabSelected(int i) {
    setState(() => _selectedIndex = i);
    _channel.invokeMethod('onTabSelected', i);
  }

  List<NavBarTab> _buildTabs() {
    final tabs = [
      const NavBarTab(
        label: '首页',
        icon: Icon(Icons.home_outlined),
        activeIcon: Icon(Icons.home),
      ),
      const NavBarTab(
        label: '应用',
        icon: Icon(Icons.list_outlined),
        activeIcon: Icon(Icons.list),
      ),
    ];

    if (_showRules) {
      tabs.add(
        const NavBarTab(
          label: '规则',
          icon: Icon(Icons.security_outlined),
          activeIcon: Icon(Icons.security),
        ),
      );
    }

    tabs.add(
      const NavBarTab(
        label: '设置',
        icon: Icon(Icons.settings_outlined),
        activeIcon: Icon(Icons.settings),
      ),
    );

    return tabs;
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
        colorScheme: _dynamicScheme!,
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        scaffoldBackgroundColor: Colors.transparent,
        colorScheme: _dynamicScheme!,
      ),
      themeMode: ThemeMode.system,
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: AnimatedSlide(
          offset: _navBarVisible ? Offset.zero : const Offset(0, 1.5),
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOutCubic,
          child: AnimatedOpacity(
            opacity: _navBarVisible ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 200),
            child: TweenAnimationBuilder<double>(
              tween: Tween<double>(begin: 0.0, end: 1.0),
              duration: const Duration(milliseconds: 600),
              curve: Curves.easeOutCubic,
              builder: (context, value, child) => Transform.translate(
                offset: Offset(0, 80 * (1 - value)),
                child: Opacity(opacity: value.clamp(0.0, 1.0), child: child),
              ),
              child: ModernCapsuleNavBar(
                selectedIndex: _selectedIndex,
                onTabSelected: _onTabSelected,
                tabs: _buildTabs(),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

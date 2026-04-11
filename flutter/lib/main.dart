import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'md3_pill_navbar.dart';

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
  static const _channel = MethodChannel('nekosu.aqnya/navbar');

  @override
  void initState() {
    super.initState();
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'setIndex' && mounted) {
        setState(() => _selectedIndex = call.arguments as int);
      }
    });
  }

  void _onTabSelected(int i) {
    setState(() => _selectedIndex = i);
    _channel.invokeMethod('onTabSelected', i);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF6750A4)),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF6750A4),
          brightness: Brightness.dark,
        ),
      ),
      themeMode: ThemeMode.system,
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: Md3PillNavBar(
          selectedIndex: _selectedIndex,
          onTabSelected: _onTabSelected,
          tabs: const [
            NavBarTab(
              label: '主页',
              icon: Icon(Icons.home_outlined),
              activeIcon: Icon(Icons.home_rounded),
            ),
            NavBarTab(
              label: '应用',
              icon: Icon(Icons.apps_outlined),
              activeIcon: Icon(Icons.apps_rounded),
            ),
            NavBarTab(
              label: '设置',
              icon: Icon(Icons.settings_outlined),
              activeIcon: Icon(Icons.settings_rounded),
            ),
          ],
        ),
      ),
    );
  }
}
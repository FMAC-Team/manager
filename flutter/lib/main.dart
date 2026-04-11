import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LiquidGlassWidgets.initialize();
  runApp(LiquidGlassWidgets.wrap(const NavBarApp()));
}

class NavBarApp extends StatefulWidget {
  const NavBarApp({super.key});
  @override
  State<NavBarApp> createState() => _NavBarAppState();
}

class _NavBarAppState extends State<NavBarApp> {
  int _selectedIndex = 0;
  late MethodChannel _channel;

  @override
  void initState() {
    super.initState();
    _channel = const MethodChannel('nav_channel');
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'setIndex') {
        setState(() => _selectedIndex = call.arguments as int);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
      useMaterial3: true,
      canvasColor: Colors.transparent, 
      scaffoldBackgroundColor: Colors.transparent,
      colorScheme: ColorScheme.fromSeed(
        seedColor: Colors.blue,
        background: Colors.transparent,
      ),
    ),
      home: Scaffold(
        backgroundColor: Colors.transparent,
        body: GlassBottomBar(
          selectedIndex: _selectedIndex,
          onTabSelected: (i) {
            setState(() => _selectedIndex = i);
            _channel.invokeMethod('onTabSelected', i);
          },
          tabs: const [
            GlassBottomBarTab(label: '主页', icon: Icon(Icons.home_rounded)),
            GlassBottomBarTab(label: '应用', icon: Icon(Icons.apps_rounded)),
            GlassBottomBarTab(label: '设置', icon: Icon(Icons.settings_rounded)),
          ],
        ),
      ),
    );
  }
}
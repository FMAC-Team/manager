import 'package:shared_preferences/shared_preferences.dart';

class DebugConfig {
  static Future<int> getNavbarStyle() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt('nav_bar_style') ?? 0;
  }

  static Future<void> setNavbarStyle(int style) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('nav_bar_style', style);
  }

  static const String _keyShowRules = 'debug_show_rules';

  static Future<bool> getShowRules() async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_keyShowRules) ?? false;
  }

  static Future<void> setShowRules(bool value) async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyShowRules, value);
   
  }

}

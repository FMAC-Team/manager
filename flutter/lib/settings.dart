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
}

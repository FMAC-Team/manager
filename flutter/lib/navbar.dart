import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class NavBarTab {
  const NavBarTab({required this.label, required this.icon, this.activeIcon});
  final String label;
  final Widget icon;
  final Widget? activeIcon;
}

class ModernCapsuleNavBar extends StatelessWidget {
  const ModernCapsuleNavBar({
    super.key,
    required this.tabs,
    required this.selectedIndex,
    required this.onTabSelected,
  });

  final List<NavBarTab> tabs;
  final int selectedIndex;
  final ValueChanged<int> onTabSelected;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final count = tabs.length;

    final double alignmentX = count > 1 ? -1.0 + (selectedIndex * 2 / (count - 1)) : 0.0;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.only(bottom: 16, left: 24, right: 24),
        child: Container(
          height: 68,
          decoration: BoxDecoration(
            color: scheme.onSurface,
            borderRadius: BorderRadius.circular(34),
            boxShadow:[
              BoxShadow(
                color: scheme.shadow.withOpacity(0.06),
                blurRadius: 24,
                spreadRadius: 2,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Stack(
            children:[
              AnimatedAlign(
                alignment: Alignment(alignmentX, 0),
                duration: const Duration(milliseconds: 400),
                curve: Curves.easeOutCubic, 
                child: FractionallySizedBox(
                  widthFactor: 1 / count,
                  child: Padding(
                    padding: const EdgeInsets.all(6.0),
                    child: Container(
                      decoration: BoxDecoration(
                        color: scheme.secondaryContainer,
                        borderRadius: BorderRadius.circular(28),
                      ),
                    ),
                  ),
                ),
              ),
              Row(
                children: List.generate(
                  count,
                  (i) => Expanded(
                    child: _TabItem(
                      tab: tabs[i],
                      selected: selectedIndex == i,
                      scheme: scheme,
                      onTap: () {
                        HapticFeedback.lightImpact();
                        onTabSelected(i);
                      },
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TabItem extends StatelessWidget {
  const _TabItem({
    required this.tab,
    required this.selected,
    required this.scheme,
    required this.onTap,
  });

  final NavBarTab tab;
  final bool selected;
  final ColorScheme scheme;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque, 
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOutCubic,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children:[
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 250),
              transitionBuilder: (child, animation) {
                return ScaleTransition(
                  scale: animation,
                  child: FadeTransition(opacity: animation, child: child),
                );
              },
              child: IconTheme(
                key: ValueKey<bool>(selected), 
                data: IconThemeData(
                  color: selected ? scheme.onSecondaryContainer : scheme.onSurfaceVariant,
                  size: selected ? 26 : 24, 
                ),
                child: (selected && tab.activeIcon != null) 
                    ? tab.activeIcon! 
                    : tab.icon,
              ),
            ),
            const SizedBox(height: 4),
            AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 250),
              style: TextStyle(
                fontSize: 11,
                fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                color: selected ? scheme.onSecondaryContainer : scheme.onSurfaceVariant,
              ),
              child: Text(
                tab.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
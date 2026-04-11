import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class NavBarTab {
  const NavBarTab({required this.label, required this.icon, this.activeIcon});
  final String label;
  final Widget icon;
  final Widget? activeIcon;
}

class Md3PillNavBar extends StatefulWidget {
  const Md3PillNavBar({
    super.key,
    required this.tabs,
    required this.selectedIndex,
    required this.onTabSelected,
  });

  final List<NavBarTab> tabs;
  final int selectedIndex;
  final ValueChanged<int> onTabSelected;

  @override
  State<Md3PillNavBar> createState() => _Md3PillNavBarState();
}

class _Md3PillNavBarState extends State<Md3PillNavBar>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _slideAnim;
  int _prevIndex = 0;

  @override
  void initState() {
    super.initState();
    _prevIndex = widget.selectedIndex;
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _slideAnim = CurvedAnimation(parent: _ctrl, curve: Curves.easeOutCubic);
  }

  @override
  void didUpdateWidget(Md3PillNavBar old) {
    super.didUpdateWidget(old);
    if (old.selectedIndex != widget.selectedIndex) {
      _prevIndex = old.selectedIndex;
      _ctrl.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final count = widget.tabs.length;

    const double barH = 80;
    const double indicatorH = 32;
    const double indicatorW = 64;
    const double pillRadius = 28.0;

    return Align(
      alignment: Alignment.bottomCenter,
      child: Padding(
        padding: const EdgeInsets.only(bottom: 16, left: 16, right: 16),
        child: Material(
          elevation: 2,
          shadowColor: scheme.shadow,
          surfaceTintColor: scheme.surfaceTint,
          color: scheme.surfaceContainer,
          borderRadius: BorderRadius.circular(pillRadius),
          child: SizedBox(
            height: barH,
            child: LayoutBuilder(
              builder: (ctx, constraints) {
                final itemW = constraints.maxWidth / count;

                return AnimatedBuilder(
                  animation: _slideAnim,
                  builder: (_, __) {
                    final fromX =
                        _prevIndex * itemW + (itemW - indicatorW) / 2;
                    final toX =
                        widget.selectedIndex * itemW + (itemW - indicatorW) / 2;
                    final indicatorX =
                        fromX + (toX - fromX) * _slideAnim.value;

                    return Stack(
                      children: [
                        // sliding indicator
                        Positioned(
                          left: indicatorX,
                          top: (barH - indicatorH) / 2,
                          width: indicatorW,
                          height: indicatorH,
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: scheme.secondaryContainer,
                              borderRadius: BorderRadius.circular(16),
                            ),
                          ),
                        ),
                        // tabs
                        Row(
                          children: List.generate(
                            count,
                            (i) => _TabItem(
                              tab: widget.tabs[i],
                              selected: widget.selectedIndex == i,
                              scheme: scheme,
                              onTap: () {
                                HapticFeedback.selectionClick();
                                widget.onTabSelected(i);
                              },
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _TabItem extends StatefulWidget {
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
  State<_TabItem> createState() => _TabItemState();
}

class _TabItemState extends State<_TabItem>
    with SingleTickerProviderStateMixin {
  late AnimationController _ctrl;
  late Animation<double> _appear;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 200),
      value: widget.selected ? 1.0 : 0.0,
    );
    _appear = CurvedAnimation(parent: _ctrl, curve: Curves.easeInOut);
  }

  @override
  void didUpdateWidget(_TabItem old) {
    super.didUpdateWidget(old);
    if (widget.selected != old.selected) {
      widget.selected ? _ctrl.forward() : _ctrl.reverse();
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = widget.scheme;
    return Expanded(
      child: InkWell(
        onTap: widget.onTap,
        borderRadius: BorderRadius.circular(28),
        splashColor: scheme.secondaryContainer.withOpacity(0.4),
        highlightColor: Colors.transparent,
        child: AnimatedBuilder(
          animation: _appear,
          builder: (_, __) {
            final iconColor = Color.lerp(
              scheme.onSurfaceVariant,
              scheme.onSecondaryContainer,
              _appear.value,
            )!;
            final labelColor = Color.lerp(
              scheme.onSurfaceVariant,
              scheme.onSurface,
              _appear.value,
            )!;
            final labelWeight = FontWeight.lerp(
              FontWeight.w400,
              FontWeight.w600,
              _appear.value,
            )!;
            return Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconTheme(
                  data: IconThemeData(color: iconColor, size: 24),
                  child: (widget.selected && widget.tab.activeIcon != null)
                      ? widget.tab.activeIcon!
                      : widget.tab.icon,
                ),
                const SizedBox(height: 4),
                Text(
                  widget.tab.label,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: labelWeight,
                    color: labelColor,
                    height: 1.0,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
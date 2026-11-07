import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MaterialApp(
    title: "Kali O'quv",
    debugShowCheckedModeBanner: false,
    theme: ThemeData.light(),
    darkTheme: ThemeData.dark(),
    themeMode: ThemeMode.system,
    home: const Shell(),
  ));
}

class Shell extends StatefulWidget {
  const Shell({super.key});
  @override
  State<Shell> createState() => _ShellState();
}

class _ShellState extends State<Shell> {
  late final WebViewController c = WebViewController()
    ..setJavaScriptMode(JavaScriptMode.unrestricted)
    ..loadFlutterAsset('assets/index.html');

  @override
  Widget build(BuildContext context) =>
      Scaffold(body: SafeArea(child: WebViewWidget(controller: c)));
}

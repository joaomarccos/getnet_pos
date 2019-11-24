import 'package:flutter/material.dart';
import 'package:getnet_pos/getnet_pos.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Teste GetNet'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String serviceStatus;

  String printerStatus;

  String mifareStatus;

  String scannerStatus;

  @override
  void initState() {
    super.initState();
    checkService();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            LabeledValue('Service Status', serviceStatus),
            LabeledValue('Printer:', printerStatus),
            LabeledValue('Mifare:', mifareStatus),
            LabeledValue('Scanner:', scannerStatus),
          ],
        ),
      ),
      floatingActionButton: Padding(
        padding: const EdgeInsets.only(bottom: 8.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: <Widget>[
            FloatingActionButton(
              onPressed: nfc,
              child: Icon(Icons.airplay),
            ),
            SizedBox(
              width: 10,
            ),
            FloatingActionButton(
              onPressed: scan,
              backgroundColor: Colors.red[900],
              child: Icon(Icons.camera_alt),
            ),
            SizedBox(
              width: 10,
            ),
            FloatingActionButton(
              onPressed: impressao,
              backgroundColor: Colors.green[900],
              child: Icon(Icons.print),
            ),
          ],
        ),
      ),
    );
  }

  void impressao() {
    setState(() {
      printerStatus = null;
    });
    GetnetPos.print(
      [
        "Header is the first line",
        "Content line 1",
        "Content line 2",
      ],
      printBarcode: false, //default is true
    )
        .then((_) => setState(() {
              printerStatus = 'Normal';
            }))
        .catchError((e) => setState(() {
              printerStatus = 'Error: ${e.code} -> ${e.message}';
            }));
  }

  void nfc() {
    setState(() {
      mifareStatus = 'Mantenha o cartão próximo!';
    });
    Future.delayed(Duration(seconds: 2), () {
      GetnetPos.getMifareCardSN()
          .then((csn) => setState(() {
                mifareStatus = 'Leitura: $csn';
              }))
          .catchError((e) => setState(() {
                mifareStatus = 'Error: ${e.code} -> ${e.message}';
              }));
    });
  }

  void checkService() async {
    var status = await GetnetPos.checkService(label: '');
    setState(() {
      serviceStatus = status;
    });
  }

  void scan() {
    setState(() {
      scannerStatus = null;
    });
    GetnetPos.scan()
        .then((result) => setState(() {
              scannerStatus = 'Leitura: $result';
            }))
        .catchError((e) => setState(() {
              debugPrint(e.toString());
              scannerStatus = 'Error: ${e.code} -> ${e.message}';
            }));
  }
}

class LabeledValue extends StatelessWidget {
  final String label;
  final String value;
  const LabeledValue(
    this.label,
    this.value, {
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: RichText(
        text: TextSpan(
          text: label,
          style: Theme.of(context).textTheme.caption.copyWith(fontWeight: FontWeight.bold),
          children: [
            TextSpan(
              text: value ?? ' SEM RESPOSTA ',
              style: Theme.of(context).textTheme.caption,
            ),
          ],
        ),
      ),
    );
  }
}

'use strict';

import * as VSCode from 'vscode';

import { workspace, ExtensionContext, window } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions } from 'vscode-languageclient';

import * as ClientUtils from "./clientutils";

export async function activate(context: ExtensionContext) {

  let logLevel = workspace.getConfiguration().get('scalaLanguageServer.logLevel')
  let javaPath = workspace.getConfiguration().get('scalaLanguageServer.javaPath')

  let clientProject = new ClientUtils.LspClientProject(
    javaPath === null ? undefined : javaPath, workspace.rootPath, logLevel
  );

  try {
    await clientProject.checkRequirements();
  } catch (e) {
    window.showErrorMessage(e);
    return;
  }

  let javaArgs: string[];
  try {
    javaArgs = await clientProject.javaArgs;
  } catch (e) {
    window.showErrorMessage(e);
    return;
  }
  let debugArgs: string[] = clientProject.debugArgs;

  let serverOptions: ServerOptions = {
    run: { command: 'java', args: javaArgs },
    debug: { command: 'java', args: debugArgs.concat(javaArgs) }
  };

  // Options to control the language client
  let clientOptions: LanguageClientOptions = {
    // Register the server for plain text documents
    documentSelector: ['scala'],
    synchronize: {
        fileEvents: workspace.createFileSystemWatcher(workspace.rootPath + '/.ensime')
    }
  };

  // Create the language client and start the client.
  let disposable = new LanguageClient('Scala Server', serverOptions, clientOptions, false).start();

  // Push the disposable to the context's subscriptions so that the
  // client can be deactivated on extension deactivation
  context.subscriptions.push(disposable);

  // Taken from the Java plugin, this configuration can't be (yet) defined in the
  //  `scala.configuration.json` file
  VSCode.languages.setLanguageConfiguration('scala', {
    onEnterRules: [
      {
        // e.g. /** | */
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        afterText: /^\s*\*\/$/,
        action: { indentAction: VSCode.IndentAction.IndentOutdent, appendText: ' * ' }
      },
      {
        // e.g. /** ...|
        beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
        action: { indentAction: VSCode.IndentAction.None, appendText: ' * ' }
      },
      {
        // e.g.  * ...|
        beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
        action: { indentAction: VSCode.IndentAction.None, appendText: '* ' }
      },
      {
        // e.g.  */|
        beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
        action: { indentAction: VSCode.IndentAction.None, removeText: 1 }
      }
    ]
  });
}

import json
from pathlib import Path


def find_config(configs, name):
    for item in configs:
        if item.get("name") == name:
            return item
    raise AssertionError(f"launch config not found: {name}")


launch = json.loads(Path(".vscode/launch.json").read_text())
collector = find_config(launch["configurations"], "Collector")
admin_server = find_config(launch["configurations"], "Admin Server")
executor = find_config(launch["configurations"], "Executor")
statistic = find_config(launch["configurations"], "Statistic")

assert collector["mainClass"] == "cc.riskswap.trader.collector.Application"
assert collector.get("projectName") == "collector"
assert admin_server.get("projectName") == "admin-server"
assert executor.get("projectName") == "executor"
assert statistic.get("projectName") == "statistic"

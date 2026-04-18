from pathlib import Path
import xml.etree.ElementTree as ET


NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def text(root: ET.Element, xpath: str) -> str:
    value = root.findtext(xpath, namespaces=NS)
    assert value is not None, f"missing xml node: {xpath}"
    return value.strip()


root_pom = ET.parse(Path("pom.xml")).getroot()
module_paths = {
    "trader-base/pom.xml": "base",
    "trader-admin/pom.xml": "admin",
    "trader-collector/pom.xml": "collector",
    "trader-executor/pom.xml": "executor",
    "trader-statistic/pom.xml": "statistic",
}

assert text(root_pom, "m:groupId") == "cc.riskswap.trader"

managed_dependency = None
for dependency in root_pom.findall("m:dependencyManagement/m:dependencies/m:dependency", NS):
    group_id = text(dependency, "m:groupId")
    artifact_id = text(dependency, "m:artifactId")
    if group_id == "cc.riskswap.trader" and artifact_id == "base":
        managed_dependency = dependency
        break

assert managed_dependency is not None, "root pom must manage cc.riskswap.trader:base"

for file_path, artifact_id in module_paths.items():
    module_pom = ET.parse(Path(file_path)).getroot()
    assert text(module_pom, "m:groupId") == "cc.riskswap.trader"
    assert text(module_pom, "m:artifactId") == artifact_id

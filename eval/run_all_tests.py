"""
Java 后端简历量化指标一键测试脚本

运行此脚本将自动执行所有测试，生成综合报告

使用方法:
python eval/run_all_tests.py

输出:
- 控制台输出：各测试结果摘要
- eval/test_report_综合.json: 完整测试报告
"""

import json
import os
import sys
import subprocess
from datetime import datetime
from typing import Dict, Any


class TestRunner:
    """测试运行器"""

    def __init__(self):
        self.results = {}
        self.base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    def run_script(self, script_path: str, description: str) -> Dict[str, Any]:
        """运行测试脚本"""
        print(f"\n{'='*70}")
        print(f"正在运行：{description}")
        print(f"脚本路径：{script_path}")
        print('='*70)

        if not os.path.exists(script_path):
            print(f"❌ 脚本不存在：{script_path}")
            return {"status": "error", "message": "脚本不存在"}

        try:
            # 运行脚本
            result = subprocess.run(
                [sys.executable, script_path],
                capture_output=True,
                text=True,
                encoding='utf-8',
                timeout=300  # 5 分钟超时
            )

            # 打印输出
            print(result.stdout)
            if result.stderr:
                print("STDERR:", result.stderr)

            return {
                "status": "success" if result.returncode == 0 else "failed",
                "returncode": result.returncode,
                "stdout": result.stdout,
                "stderr": result.stderr
            }

        except subprocess.TimeoutExpired:
            print(f"❌ 测试超时（>5 分钟）")
            return {"status": "timeout", "message": "测试超时"}
        except Exception as e:
            print(f"❌ 运行错误：{e}")
            return {"status": "error", "message": str(e)}

    def run_ragas_test(self) -> Dict[str, Any]:
        """运行 RAGAS 测试"""
        script = os.path.join(self.base_dir, "LangChain-RAG", "eval", "ragas_evaluation.py")
        return self.run_script(script, "RAGAS 检索质量评估")

    def run_sse_test(self) -> Dict[str, Any]:
        """运行 SSE 延迟测试"""
        script = os.path.join(self.base_dir, "HumanResourceOfficial", "eval", "benchmark_sse.py")
        return self.run_script(script, "SSE 流式输出延迟测试")

    def run_ab_test(self) -> Dict[str, Any]:
        """运行 A/B 测试"""
        script = os.path.join(self.base_dir, "HumanResourceOfficial", "eval", "interview_assignment_ab_test.py")
        return self.run_script(script, "面试调度算法 A/B 测试")

    def run_all(self):
        """运行所有测试"""
        print("\n" + "="*70)
        print("Java 后端简历量化指标测试套件")
        print("="*70)
        print(f"开始时间：{datetime.now().isoformat()}")
        print("\n即将运行以下测试:")
        print("1. RAGAS 检索质量评估")
        print("2. SSE 流式输出延迟测试")
        print("3. 面试调度算法 A/B 测试")
        print("\n注意：JMeter 缓存测试需要手动执行")
        print("="*70)

        # 运行各项测试
        self.results["ragas"] = self.run_ragas_test()
        self.results["sse"] = self.run_sse_test()
        self.results["ab_test"] = self.run_ab_test()

        # 生成综合报告
        self.generate_report()

    def generate_report(self):
        """生成综合测试报告"""
        report = {
            "timestamp": datetime.now().isoformat(),
            "tests": {}
        }

        # 解析各项测试结果
        for test_name, result in self.results.items():
            report["tests"][test_name] = {
                "status": result.get("status", "unknown"),
                "timestamp": datetime.now().isoformat()
            }

        # 保存报告
        output_path = os.path.join(self.base_dir, "HumanResourceOfficial", "eval", "test_report_综合.json")
        os.makedirs(os.path.dirname(output_path), exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)

        print("\n" + "="*70)
        print("测试完成!")
        print("="*70)
        print(f"综合报告已保存至：{output_path}")

        # 打印摘要
        print("\n测试结果摘要:")
        for test_name, result in self.results.items():
            status = result.get("status", "unknown")
            icon = "✅" if status == "success" else "❌"
            print(f"  {icon} {test_name}: {status}")


def main():
    """主函数"""
    runner = TestRunner()
    runner.run_all()


if __name__ == "__main__":
    main()

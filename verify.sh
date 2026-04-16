#!/bin/bash

# AllSleep Verification Harness (v2 - Test Added)
# 이 스크립트는 프로젝트의 컴파일, 품질(Lint), 그리고 단위 테스트를 통합 검증합니다.

echo "🚀 [Harness] 정밀 검증을 시작합니다..."

# 1. 컴파일 및 기본 타입 체크
echo "📦 1/3: 타입 및 문법 검사 중 (compileDebugKotlinAndroid)..."
./gradlew :composeApp:compileDebugKotlinAndroid --parallel --build-cache
if [ $? -ne 0 ]; then
    echo "❌ [Error] 컴파일 또는 타입 검사에 실패했습니다."
    exit 1
fi

# 2. 정적 분석 (Lint)
echo "🔍 2/3: 코드 스타일 및 잠재적 버그 검증 중 (lintDebug)..."
./gradlew :composeApp:lintDebug --parallel --build-cache
if [ $? -ne 0 ]; then
    echo "❌ [Error] 린트 검사에서 문제가 발견되었습니다."
    exit 1
fi

# 3. 단위 테스트 (Unit Test)
echo "🧪 3/3: 단위 테스트 실행 중 (testDebugUnitTest)..."
./gradlew :composeApp:testDebugUnitTest --parallel --build-cache
if [ $? -ne 0 ]; then
    echo "❌ [Error] 단위 테스트 중 실패한 항목이 있습니다."
    exit 1
fi

echo "✅ [Success] 컴파일/린트/테스트가 모두 Green입니다!"
exit 0

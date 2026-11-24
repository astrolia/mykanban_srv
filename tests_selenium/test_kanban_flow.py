# ==============================================================================
# Suíte de Testes Automatizados com Selenium para o Projeto Kanban
# ==============================================================================
# Pré-requisitos:
#   1. Backend Spring Boot rodando em http://localhost:8080
#   2. Frontend React rodando em http://localhost:5173
# Execução:
#   python "tests_selenium/test_kanban_flow.py"
# ==============================================================================

import time
import uuid
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

BASE_URL = "http://localhost:5173"

def build_driver():
    options = Options()
    options.add_argument("--start-maximized")
    options.add_argument("--disable-notifications")
    options.add_argument("--disable-popup-blocking")
    driver = webdriver.Chrome(options=options)
    driver.implicitly_wait(5)
    return driver

def set_input_value(element, text):
    """
    Limpa de forma confiável inputs controlados pelo React (MUI)
    usando CONTROL+A + BACKSPACE antes de digitar o novo texto.
    """
    element.click()
    element.send_keys(Keys.CONTROL + "a")
    element.send_keys(Keys.BACKSPACE)
    if text:
        element.send_keys(text)

def test_full_kanban_flow():
    driver = build_driver()
    wait = WebDriverWait(driver, 10)
    
    unique_suffix = str(uuid.uuid4())[:6]
    test_user_name = f"Aluno Teste {unique_suffix}"
    test_user_email = f"aluno_{unique_suffix}@teste.com"
    test_user_password = "senhaSegura123"
    
    print("\n=======================================================")
    print(" INICIANDO TESTES DO FRONT-END KANBAN COM SELENIUM")
    print("=======================================================")
    
    try:
        # -------------------------------------------------------------
        # TESTE 1: Cadastro de Usuário (/cadastro)
        # -------------------------------------------------------------
        print("\n[1/5] Testando Cadastro de Usuário...")
        driver.get(f"{BASE_URL}/cadastro")
        
        name_input = wait.until(EC.presence_of_element_located((By.NAME, "name")))
        set_input_value(name_input, test_user_name)
        
        email_input = driver.find_element(By.NAME, "email")
        set_input_value(email_input, test_user_email)
        
        pass_input = driver.find_element(By.NAME, "password")
        set_input_value(pass_input, test_user_password)
        
        confirm_pass_input = driver.find_element(By.NAME, "confirmPassword")
        set_input_value(confirm_pass_input, test_user_password)
        
        submit_btn = driver.find_element(By.XPATH, "//button[@type='submit' and contains(., 'Confirmar')]")
        submit_btn.click()
        
        wait.until(EC.url_contains("/login"))
        print("✓ Cadastro realizado com sucesso! Redirecionado para /login.")

        # -------------------------------------------------------------
        # TESTE 2: Tentativa de Login com Senha Incorreta
        # -------------------------------------------------------------
        print("\n[2/5] Testando Login com Credenciais Inválidas...")
        driver.get(f"{BASE_URL}/login")
        
        email_login = wait.until(EC.presence_of_element_located((By.NAME, "email")))
        set_input_value(email_login, test_user_email)
        
        pass_login = driver.find_element(By.NAME, "password")
        set_input_value(pass_login, "senhaErrada123")
        
        login_btn = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[@type='submit' and contains(., 'Confirmar')]"))
        )
        login_btn.click()
        
        time.sleep(1.2)
        assert "/login" in driver.current_url, "Não deveria sair da tela de login com credenciais inválidas"
        pass_val = driver.find_element(By.NAME, "password").get_attribute("value")
        assert pass_val == "", "O campo de senha deveria ter sido limpo após erro de login"
        print("✓ Login inválido tratado corretamente (permaneceu em /login e senha foi limpa)!")

        # -------------------------------------------------------------
        # TESTE 3: Login Válido e Armazenamento do JWT
        # -------------------------------------------------------------
        print("\n[3/5] Testando Login com Sucesso...")
        
        # Limpa e digita novamente os dados corretos
        email_login = wait.until(EC.presence_of_element_located((By.NAME, "email")))
        set_input_value(email_login, test_user_email)
        
        pass_login = driver.find_element(By.NAME, "password")
        set_input_value(pass_login, test_user_password)
        
        login_btn = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[@type='submit' and contains(., 'Confirmar')]"))
        )
        login_btn.click()
        
        wait.until(EC.url_contains("/home"))
        print("✓ Login bem-sucedido! Redirecionado para /home.")
        
        # Valida que o token JWT retornado pelo Spring Boot foi gravado no localStorage
        wait.until(lambda d: d.execute_script("return window.localStorage.getItem('token');"))
        jwt_token = driver.execute_script("return window.localStorage.getItem('token');")
        assert jwt_token is not None and len(jwt_token) > 20, "Token JWT não foi gravado no localStorage!"
        print(f"✓ Token JWT gravado no localStorage: {jwt_token[:18]}...")

        # -------------------------------------------------------------
        # TESTE 4: Criação de Ambiente
        # -------------------------------------------------------------
        print("\n[4/5] Testando Criação de Novo Ambiente...")
        time.sleep(1)
        
        novo_ambiente_btn = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[contains(., 'Criar novo ambiente')]"))
        )
        novo_ambiente_btn.click()
        
        # Localiza o campo de nome do ambiente no modal
        env_name_input = wait.until(
            EC.presence_of_element_located(
                (By.XPATH, "//label[contains(., 'Nome do ambiente')]/following-sibling::div//input")
            )
        )
        env_test_name = f"Ambiente {unique_suffix}"
        set_input_value(env_name_input, env_test_name)
        
        criar_env_submit = wait.until(
            EC.element_to_be_clickable((By.XPATH, "//button[@type='submit' and contains(., 'Criar ambiente')]"))
        )
        criar_env_submit.click()
        
        # Aguarda o novo ambiente renderizar na listagem do Home
        wait.until(
            EC.presence_of_element_located((By.XPATH, f"//*[contains(text(), '{env_test_name}')]"))
        )
        print(f"✓ Ambiente '{env_test_name}' criado e renderizado com sucesso no Grid!")

        # -------------------------------------------------------------
        # TESTE 5: Acessar Ambiente e Verificar o Quadro Kanban
        # -------------------------------------------------------------
        print("\n[5/5] Testando Entrada no Ambiente e Abertura do Kanban...")
        env_card = wait.until(
            EC.element_to_be_clickable((By.XPATH, f"//*[contains(text(), '{env_test_name}')]"))
        )
        env_card.click()
        
        wait.until(EC.url_contains("/environment/"))
        print(f"✓ Navegação para o Kanban confirmada ({driver.current_url}).")
        
        time.sleep(2)
        print("\n=======================================================")
        print(" TODOS OS 5 TESTES FORAM EXECUTADOS COM SUCESSO!")
        print("=======================================================\n")
        
    except Exception as e:
        print(f"\n❌ FALHA DURANTE O TESTE SELENIUM: {e}")
        print(f"URL atual: {driver.current_url}")
        raise
    finally:
        driver.quit()

if __name__ == "__main__":
    test_full_kanban_flow()

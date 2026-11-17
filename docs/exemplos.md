## Exemplos

Os exemplos usam a aplicação rodando localmente no perfil `offline` (`http://localhost:8082/api`). Os valores de saída foram obtidos com a base de dados V0059.

### Cálculo com observabilidade

A operação abaixo é uma NF-e (`tpDoc` 55) de empresa do Simples Nacional (`CRT` 1) com quatro itens: dois itens de revenda de mercadorias (atividade `01`), um deles com Imposto Seletivo; um item com atividade inexistente; e um item com base de cálculo inválida.

**Entrada** (`operacao.json`):

```json
{
  "id": "teste-1",
  "versao": "1.0.0",
  "data": "2027-03-15",
  "tpDoc": "55",
  "CRT": "1",
  "itens": [
    { "numero": 1, "cAtivSN": "01", "cClassTrib": "000001", "baseCalculo": "1000.00",
      "valorImpostoSeletivo": "0.00", "pIBSSN": "1.25", "pCBSSN": "2.75" },
    { "numero": 2, "cAtivSN": "01", "cClassTrib": "000001", "baseCalculo": "500.00",
      "valorImpostoSeletivo": "50.00", "pIBSSN": "1.25", "pCBSSN": "2.75" },
    { "numero": 3, "cAtivSN": "99", "cClassTrib": "000001", "baseCalculo": "100.00",
      "valorImpostoSeletivo": "0.00" },
    { "numero": 4, "cAtivSN": "01", "cClassTrib": "000001", "baseCalculo": "abc",
      "valorImpostoSeletivo": "0.00", "pIBSSN": "1.25", "pCBSSN": "2.75" }
  ]
}
```

Envio da operação para cálculo:

```bash
curl -X POST http://localhost:8082/api/calculadora/observabilidade/simples-nacional \
  -H 'Content-Type: application/json' -d @operacao.json
```

Resposta:

```json
{
  "oper": { "data": "2027-03-15", "tpDoc": "55", "CRT": "1" },
  "objetos": [
    { "nObj": 1, "estadoItem": "CALCULADO", "tribCalc": {
        "vRBSNitem": "1000.00", "tpRBSN": 1, "tpRBSNDescricao": "Receita bruta - interna",
        "vBC": "1000.00", "pIBSSN": "1.25", "vIBSSN": "12.50", "vIBSPendSusp": "0.00",
        "pCBSSN": "2.75", "vCBSSN": "27.50", "vCBSPendSusp": "0.00",
        "IS": { "valor": "0.00" }, "totalItem": "40.00" } },
    { "nObj": 2, "estadoItem": "CALCULADO", "tribCalc": {
        "vRBSNitem": "500.00", "tpRBSN": 1, "tpRBSNDescricao": "Receita bruta - interna",
        "vBC": "500.00", "pIBSSN": "1.25", "vIBSSN": "6.25", "vIBSPendSusp": "0.00",
        "pCBSSN": "2.75", "vCBSSN": "13.75", "vCBSPendSusp": "0.00",
        "IS": { "valor": "50.00" }, "totalItem": "70.00" } },
    { "nObj": 3, "estadoItem": "NAO_CALCULADO", "tribCalc": {
        "vRBSNitem": "100.00", "tpRBSN": 1, "tpRBSNDescricao": "Receita bruta - interna" } },
    { "nObj": 4, "estadoItem": "INCONSISTENCIA_ENTRADA", "erros": [{
        "type": "http://localhost:8082/api/calculadora/observabilidade/simples-nacional/erros?codigo=VSN-002",
        "title": "Base de cálculo do Simples inválida (negativa/não numérica)",
        "code": "VSN-002",
        "pointer": "#/itens/3/baseCalculo",
        "detail": "O valor não é um número decimal válido, recebido: 'abc'." }] }
  ],
  "total": {
    "vRBSNtot": "1600.00", "vBC": "1500.00",
    "vIBSSNtot": "18.75", "vIBSSNtotPendSusp": "0.00",
    "vCBSSNtot": "41.25", "vCBSSNtotPendSusp": "0.00",
    "totalIs": "50.00", "totalTributos": "110.00"
  }
}
```

- Erros de item não interrompem os demais: a resposta é HTTP 200 e cada item traz seu `estadoItem` (`CALCULADO`, `NAO_CALCULADO`, `INCONSISTENCIA_ENTRADA`, `NAO_IMPLEMENTADO` ou `FALHA_TECNICA`).
- Erros globais da operação (por exemplo `id`/`versao` ausentes, MEI com `CRT` 4, `tpDoc` não suportado na data) rejeitam a operação inteira com HTTP 422.
- Atividade (`cAtivSN`) ausente, desconhecida ou não admissível para o `tpDoc` deixa o item como `NAO_CALCULADO`, sem erro.
- O valor do Imposto Seletivo não compõe a receita bruta do item; é apenas repassado e somado ao `totalItem`.

### Variações do regime

| Cenário | Campos da operação | Efeito no item do exemplo (base 1000,00) |
|---|---|---|
| Excesso de sublimite | `"CRT": "2"` | Calcula apenas a CBS (27,50); o IBS não sai na resposta |
| Apuração por fora | `"CRT": "3", "tpRegimeIBSCBS": "2"` | IBS e CBS vão para `vIBSPendSusp` (12,50) e `vCBSPendSusp` (27,50); `totalItem` 0,00 |
| Compra governamental | `"tpEnteGov": "2"` com `"data": "2030-03-15"` | 20% da CBS é deslocada para o IBS: `pCBSSN` 2,20 e `pIBSSN` 1,80 |

### Consultas

```bash
B=http://localhost:8082/api
# Atividades vigentes na data (filtros opcionais: anexos, tpDoc, cClassTrib)
curl "$B/calculadora/simples-nacional/atividades?data=2027-03-15&tpDoc=55"
# Uma atividade pelo código
curl "$B/calculadora/simples-nacional/atividades/01?data=2027-03-15"
# Descrição de um código de erro
curl "$B/calculadora/observabilidade/simples-nacional/erros?codigo=VSN-002"
# Versão da aplicação e da base de dados
curl "$B/versao"
```

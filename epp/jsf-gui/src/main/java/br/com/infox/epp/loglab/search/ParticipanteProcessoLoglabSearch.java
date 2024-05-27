package br.com.infox.epp.loglab.search;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import br.com.infox.cdi.dao.Dao;
import br.com.infox.cdi.qualifier.GenericDao;
import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.core.util.StringUtil;
import br.com.infox.epp.loglab.dto.ParticipanteProcessoLogLabDTO;
import br.com.infox.epp.loglab.vo.EmpresaVO;
import br.com.infox.epp.loglab.vo.ServidorContribuinteVO;
import br.com.infox.epp.pessoa.entity.PessoaAnonima;
import br.com.infox.epp.pessoa.type.TipoPessoaEnum;
import br.com.infox.epp.processo.entity.Processo_;
import br.com.infox.epp.processo.partes.entity.ParticipanteProcesso;
import br.com.infox.epp.processo.partes.entity.ParticipanteProcesso_;
import br.com.infox.epp.processo.partes.entity.TipoParte_;
import br.com.infox.jsf.converter.CnpjConverter;
import br.com.infox.jsf.converter.CpfConverter;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ParticipanteProcessoLoglabSearch extends PersistenceController {

    @Inject
    private EmpresaSearch empresaSearch;
    @Inject
    private ServidorContribuinteSearch servidorContribuinteSearch;
    @Inject
    @GenericDao
    private Dao<PessoaAnonima, Integer> pessoaAnonimaDao;

//    public List<ParticipanteProcessoLogLabDTO> getListaParticipanteProcessoLoglabDTOByCodTipoParteAndIdProcesso(
//            Integer idProcesso, String codTipoParte) {
//        List<ParticipanteProcesso> listaParticipante = getListaParticipanteProcessoByCodTipoParteAndIdProcesso(
//                idProcesso, codTipoParte);
//
//        List<ParticipanteProcessoLogLabDTO> listaDTO = new ArrayList<>();
//        for (ParticipanteProcesso participante : listaParticipante) {
//            if(TipoPessoaEnum.A.equals(participante.getPessoa().getTipoPessoa())) {
//                PessoaAnonima pessoaAnonima = pessoaAnonimaDao.findById(participante.getPessoa().getIdPessoa());
//                ParticipanteProcessoLogLabDTO dto = new ParticipanteProcessoLogLabDTO();
//                dto.setNome(pessoaAnonima.getNome());
//                dto.setTelefoneCelular(pessoaAnonima.getTelefone());
//                listaDTO.add(dto);
//            } else if(participante.getPessoa().getCodigo() != null){
//                String codigo = participante.getPessoa().getCodigo();
//    			if (codigo.length() == 14) {
//                    ParticipanteProcessoLogLabDTO dto = participanteDTOEmpresa(codigo);
//                    if (StringUtils.isBlank(dto.getCnpj())) {
//                    	dto.setCpf(CnpjConverter.format(codigo));
//                    	dto.setNome(participante.getNome());
//                    	dto.setNomeFantasia(participante.getNome());
//                    	dto.setRazaoSocial(participante.getNome());
//                    }
//    				listaDTO.add(dto);
//                } else if (codigo.length() == 11) {
//                    ParticipanteProcessoLogLabDTO dto = participanteDTOServidorContribuinte(codigo);
//                    if (StringUtils.isBlank(dto.getCpf())) {
//                    	dto.setCpf(CpfConverter.format(codigo));
//                    	dto.setNome(participante.getNome());
//                    }
//                    dto.setTipoPessoa(TipoPessoaEnum.F);
//    				listaDTO.add(dto);
//                }
//            }
//        }
//
//        return listaDTO;
//    }
//
//    private ParticipanteProcessoLogLabDTO participanteDTOServidorContribuinte(String cpf) {
//        ParticipanteProcessoLogLabDTO participanteDTO = new ParticipanteProcessoLogLabDTO();
//        participanteDTO.setTipoPessoa(TipoPessoaEnum.F);
//        ServidorContribuinteVO servidorVO = servidorContribuinteSearch.getServidorByCPF(cpf);
//        if (servidorVO != null) {
//            participanteDTO.setNome(servidorVO.getNomeCompleto());
//            participanteDTO.setEmail(servidorVO.getEmail());
//            participanteDTO.setTelefoneFixo(servidorVO.getTelefone());
//            participanteDTO.setTelefoneCelular(servidorVO.getCelular());
//            participanteDTO.setDataExercicio(servidorVO.getDataExercicio());
//            participanteDTO.setDataPosse(servidorVO.getDataPosse());
//            participanteDTO.setDataNomeacao(servidorVO.getDataNomeacao());
//            participanteDTO.setCpf(CpfConverter.format(servidorVO.getCpf()));
//            participanteDTO.setMatricula(servidorVO.getMatricula());
//            participanteDTO.setCargo(!StringUtil.isEmpty(servidorVO.getCargoCarreira()) ? servidorVO.getCargoCarreira()
//                    : servidorVO.getCargoComissao());
//            participanteDTO
//                    .setFuncao(!StringUtil.isEmpty(servidorVO.getOcupacaoCarreira()) ? servidorVO.getOcupacaoCarreira()
//                            : servidorVO.getOcupacaoComissao());
//            participanteDTO.setOrgaoLotado(servidorVO.getOrgao());
//        } else {
//            ServidorContribuinteVO contribuinte = servidorContribuinteSearch.getContribuinteByCPF(cpf);
//            if (contribuinte != null) {
//                participanteDTO.setEmail(contribuinte.getEmail());
//                participanteDTO.setTelefoneFixo(contribuinte.getTelefone());
//                participanteDTO.setTelefoneCelular(contribuinte.getCelular());
//                participanteDTO.setNome(contribuinte.getNomeCompleto());
//                participanteDTO.setCpf(CpfConverter.format(contribuinte.getCpf()));
//            }
//        }
//        return participanteDTO;
//    }
//
//    private ParticipanteProcessoLogLabDTO participanteDTOEmpresa(String cnpj) {
//        EmpresaVO empresaVO = empresaSearch.getEmpresaVOByCnpj(cnpj);
//        ParticipanteProcessoLogLabDTO participanteDTO = new ParticipanteProcessoLogLabDTO();
//        if (empresaVO == null) {
//            return participanteDTO;
//        }
//        participanteDTO.setEmail(empresaVO.getEmail());
//        participanteDTO.setTelefoneFixo(empresaVO.getTelefoneFixo());
//        participanteDTO.setTelefoneCelular(empresaVO.getTelefoneCelular());
//        participanteDTO.setTipoPessoa(TipoPessoaEnum.J);
//        participanteDTO.setNome(empresaVO.getRazaoSocial());
//        participanteDTO.setNomeFantasia(empresaVO.getNomeFantasia());
//        participanteDTO.setCnpj(CnpjConverter.format(empresaVO.getCnpj()));
//        participanteDTO.setRazaoSocial(empresaVO.getRazaoSocial());
//        return participanteDTO;
//    }
//
//    private List<ParticipanteProcesso> getListaParticipanteProcessoByCodTipoParteAndIdProcesso(Integer idProcesso,
//            String codTipoParte) {
//        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
//        CriteriaQuery<ParticipanteProcesso> query = cb.createQuery(ParticipanteProcesso.class);
//        Root<ParticipanteProcesso> participante = query.from(ParticipanteProcesso.class);
//        query.select(participante);
//        query.where(cb.and(cb.isTrue(participante.get(ParticipanteProcesso_.ativo)),
//                cb.equal(participante.get(ParticipanteProcesso_.processo).get(Processo_.idProcesso), idProcesso),
//                cb.equal(participante.get(ParticipanteProcesso_.tipoParte).get(TipoParte_.identificador),
//                        codTipoParte)));
//
//        return getEntityManager().createQuery(query).getResultList();
//    }

}

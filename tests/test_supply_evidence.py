import copy,csv,io,json,subprocess,sys,tempfile,unittest
from pathlib import Path
import supply_evidence as v


def valid_row(case=0,n=2):
    initial,orders=v.scenario(v.EVAL[0],case,n)
    # Scripted reference fixture only; never supplies actions/labels to training.
    actions=[]
    for step in range(4):
        for code in initial:
            actions.append(code%2+1 if step<2 and code//2==(case%2)^step else 0)
    result=v.replay(initial,case%2,actions,orders)
    row=dict(zip(v.FIELDS,['visible',str(v.SEEDS[0]),'0',str(v.EVAL[0]),str(n),str(case),'0','0','a'*64,str(case%2),
                          ','.join(map(str,initial)),','.join(map(str,actions)),','.join(map(str,orders)),
                          *[','.join(map(str,result[k])) for k in ('service_steps','consumed','bank','remaining','deposited','withdrawn')],str(result['wrong'])]))
    return row


class SupplyEvidenceTest(unittest.TestCase):
    def test_reference_and_balanced_scenarios(self):
        for n in v.POPULATIONS:
            for case in range(256):
                row=valid_row(case,n);_,result,_=v.validate_trial(row)
                self.assertEqual(result['consumed'],[n//2,n//2]);self.assertEqual(result['wrong'],0)
                self.assertEqual(sorted(result['service_steps']),[1,2])

    def test_private_roundtrip_and_waiting(self):
        r=v.replay([0,2],0,[0,1,0,3,0,1,0,3],[0,1]*4,check_masks=False)
        self.assertEqual(r['consumed'],[0,0]);self.assertEqual(r['remaining'],[1,0,0,1]);self.assertEqual(r['wrong'],2)
        r=v.replay([0,2],0,[0,1,3,3,0,0,0,0],[0,1]*4,check_masks=False)
        self.assertEqual(r['remaining'],[1,1,0,0]);self.assertEqual(r['bank'],[0,0])

    def test_forged_fields(self):
        fields={'service_steps':'1,1','consumed':'2,1','bank':'1,0','remaining':'1,0,0,0',
                'deposited':'0,0,0,0','withdrawn':'1,0,0,0','wrong':'1','policy_sha256':'oops','first':'1',
                'initial':'0,0','population':'3','case':'256','updates':'-1','samples':'00','learning_seed':'5'}
        for field,value in fields.items():
            with self.subTest(field=field):
                row=valid_row();row[field]=value
                with self.assertRaises(ValueError):v.validate_trial(row)

    def test_bad_trace(self):
        for field,index,new in [('actions',0,4),('actions',7,3),('orders',1,0),('initial',0,9)]:
            row=valid_row();values=row[field].split(',');values[index]=values[0] if field=='orders' else str(new);row[field]=','.join(values)
            with self.subTest(field=field,index=index),self.assertRaises(ValueError):v.validate_trial(row)

    def test_wrong_schedule(self):
        row=valid_row();row['eval_seed']=str(v.EVAL[1])
        # At least one of the many keyed scenarios must disagree; use eight members.
        row=valid_row(27,8);row['eval_seed']=str(v.EVAL[1])
        with self.assertRaisesRegex(ValueError,'identity'):v.validate_trial(row)

    def test_extra_and_missing(self):
        row=valid_row();row['extra']='0'
        with self.assertRaises(ValueError):v.validate_trial(row)
        row=valid_row();del row['remaining']
        with self.assertRaises(ValueError):v.validate_trial(row)

    def test_bounded_integer(self):
        for bad in ('','+1','-1','01',' 1','1.0','1e0','NaN','9'*17):
            with self.subTest(value=bad),self.assertRaises(ValueError):v.integer(bad)

    def test_completion_is_not_a_matrix(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp);(path/'identity.txt').write_text('synthetic-communal-supply-memory-only\n')
            (path/'completed.txt').write_text('All declared trajectories and evaluation cases completed. Learning gates require independent validation.\n')
            (path/'updates.tsv').write_text('\t'.join(v.UPDATE_FIELDS)+'\n')
            with self.assertRaisesRegex(ValueError,'incomplete learning matrix'):v.validate(path)

    def test_no_assert_removal_under_optimized_python(self):
        code="import supply_evidence as v; r="+repr(valid_row())+"; r['consumed']='2,1'; v.validate_trial(r)"
        for option in ('-O','-OO'):
            run=subprocess.run([sys.executable,option,'-c',code],cwd=Path(__file__).parent,capture_output=True,text=True)
            self.assertNotEqual(run.returncode,0);self.assertIn('forged consumed',run.stderr)

    def test_source_never_exports_synthetic_policy(self):
        root=Path(__file__).resolve().parent
        for path in (root/'commonslearning').rglob('*.java'):
            s=path.read_text()
            for forbidden in ('PolicyFile.','TrainingState.','CheckpointTool.','org.bukkit'):
                self.assertNotIn(forbidden,s,path.name)

if __name__=='__main__':unittest.main()
